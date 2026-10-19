#include <OneWire.h>
#include <DallasTemperature.h>
#include <DHT.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <FS.h>
#include <LittleFS.h>

// =========================================================================
// GRAINGUARDIAN ESP32 FIRMWARE v2.0 - INDUSTRIAL EDGE IoT
// Multi-Depth Thermal Stratification, Capacitive Moisture & LittleFS Logging
// =========================================================================

// -------- HARDWARE PIN CONFIGURATION --------
#define DS18B20_PIN 4      // 1-Wire bus with external 4.7k ohm pull-up to 3.3V
#define MOISTURE_PIN 34    // SEN0193 Capacitive Moisture Sensor (ADC1_CH6 exclusively)
#define DHT_PIN 27         // DHT11 Ambient Temperature & Humidity
#define DHT_TYPE DHT11

// -------- BLE SERVICE & CHARACTERISTIC UUIDs --------
#define SERVICE_UUID        "12345678-1234-1234-1234-1234567890AB"
#define CHARACTERISTIC_UUID "12345678-1234-1234-1234-1234567890AC"

// -------- LOGGING CONFIGURATION --------
#define HISTORY_FILE "/grain_history.bin"
#define LOG_INTERVAL_MS 300000L   // 5 minutes (300,000 ms) in autonomous mode
#define LIVE_INTERVAL_MS 2000L    // 2 seconds during active BLE connection

// Fixed 16-byte packed binary structure for LittleFS
struct __attribute__((packed)) GrainRecord {
    uint32_t seq_id;      // Monotonically increasing sequence number
    uint32_t uptime_sec;  // Seconds since boot
    int16_t t1_centic;    // Temperature in centi-degrees Celsius (-12700 for disconnected)
    int16_t t2_centic;    // Temperature in centi-degrees Celsius
    int16_t t3_centic;    // Temperature in centi-degrees Celsius
    uint16_t moisture_mv; // Analog millivolts from ADC1 (0 - 3300)
    uint8_t humidity;     // 0 - 100% RH
    uint8_t status_flags; // Bit0: T1_OK, Bit1: T2_OK, Bit2: T3_OK, Bit3: M_OK, Bit4: DHT_OK
};

// -------- SENSOR OBJECTS & STATE --------
OneWire oneWire(DS18B20_PIN);
DallasTemperature ds18b20(&oneWire);
DHT dht(DHT_PIN, DHT_TYPE);

BLECharacteristic *sensorCharacteristic;
bool deviceConnected = false;
bool oldDeviceConnected = false;
uint32_t nextSequenceId = 1;
unsigned long lastLogTime = 0;
unsigned long lastLiveTime = 0;
bool littleFsReady = false;

// -------- BLE CALLBACKS & COMMAND HANDLER --------
class GrainGuardianCallbacks : public BLEServerCallbacks {
    void onConnect(BLEServer *server) override {
        deviceConnected = true;
        Serial.println("[BLE] GrainGuardian Android App Connected!");
    }

    void onDisconnect(BLEServer *server) override {
        deviceConnected = false;
        Serial.println("[BLE] Phone disconnected. Preserving autonomous logging.");
    }
};

void sendBLE(String message) {
    if (deviceConnected && sensorCharacteristic != nullptr) {
        sensorCharacteristic->setValue(message.c_str());
        sensorCharacteristic->notify();
        delay(40);
    }
}

// -------- LITTLEFS PERSISTENCE ENGINE --------
void initStorage() {
    if (!LittleFS.begin(true)) {
        Serial.println("[LittleFS] Mounting Failed. Autonomous logging disabled.");
        littleFsReady = false;
        return;
    }
    littleFsReady = true;
    Serial.println("[LittleFS] Mounted successfully.");

    // Scan existing records to determine nextSequenceId
    if (LittleFS.exists(HISTORY_FILE)) {
        File file = LittleFS.open(HISTORY_FILE, FILE_READ);
        if (file) {
            size_t count = file.size() / sizeof(GrainRecord);
            Serial.printf("[LittleFS] Found %d existing offline records (%d bytes).\n", count, file.size());
            if (count > 0) {
                file.seek((count - 1) * sizeof(GrainRecord));
                GrainRecord lastRec;
                if (file.read((uint8_t*)&lastRec, sizeof(GrainRecord)) == sizeof(GrainRecord)) {
                    nextSequenceId = lastRec.seq_id + 1;
                }
            }
            file.close();
        }
    }
}

void appendRecord(const GrainRecord &rec) {
    if (!littleFsReady) return;
    File file = LittleFS.open(HISTORY_FILE, FILE_APPEND);
    if (!file) {
        Serial.println("[LittleFS] Failed to open history file for appending");
        return;
    }
    file.write((const uint8_t*)&rec, sizeof(GrainRecord));
    file.close();
    Serial.printf("[LittleFS] Appended record #%u (MV=%u, T1=%d.%02d)\n",
        rec.seq_id, rec.moisture_mv, rec.t1_centic / 100, abs(rec.t1_centic % 100));
}

// Synchronize historical records over BLE to Android client
void syncHistoryOverBle() {
    if (!littleFsReady || !deviceConnected) return;

    if (!LittleFS.exists(HISTORY_FILE)) {
        sendBLE("SYNC:EMPTY");
        return;
    }

    File file = LittleFS.open(HISTORY_FILE, FILE_READ);
    if (!file) {
        sendBLE("SYNC:ERR_READ");
        return;
    }

    size_t totalRecords = file.size() / sizeof(GrainRecord);
    sendBLE("SYNC:START,TOTAL=" + String(totalRecords));

    GrainRecord rec;
    size_t sentCount = 0;
    while (file.read((uint8_t*)&rec, sizeof(GrainRecord)) == sizeof(GrainRecord)) {
        if (!deviceConnected) break; // Client disconnected mid-transfer

        String line = "HIST:SEQ=" + String(rec.seq_id) +
                      ",UP=" + String(rec.uptime_sec) +
                      ",T1=" + (rec.t1_centic == -12700 ? "ERR" : String(rec.t1_centic / 100.0, 2)) +
                      ",T2=" + (rec.t2_centic == -12700 ? "ERR" : String(rec.t2_centic / 100.0, 2)) +
                      ",T3=" + (rec.t3_centic == -12700 ? "ERR" : String(rec.t3_centic / 100.0, 2)) +
                      ",MV=" + String(rec.moisture_mv) +
                      ",H=" + String(rec.humidity) +
                      ",F=" + String(rec.status_flags);

        sendBLE(line);
        sentCount++;
    }
    file.close();

    sendBLE("SYNC:END,SENT=" + String(sentCount));
}

// Write callback for incoming Android commands
class CommandCallbacks : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *characteristic) override {
        String rxValue = String(characteristic->getValue().c_str());
        rxValue.trim();
        Serial.println("[BLE CMD] Received: " + rxValue);

        if (rxValue == "CMD:INFO") {
            size_t recCount = 0;
            if (littleFsReady && LittleFS.exists(HISTORY_FILE)) {
                File f = LittleFS.open(HISTORY_FILE, FILE_READ);
                if (f) {
                    recCount = f.size() / sizeof(GrainRecord);
                    f.close();
                }
            }
            sendBLE("INFO:VER=2.0;RECS=" + String(recCount) + ";UP=" + String(millis() / 1000));
        }
        else if (rxValue == "CMD:SYNC") {
            syncHistoryOverBle();
        }
        else if (rxValue == "CMD:CLEAR_HISTORY") {
            if (littleFsReady) {
                LittleFS.remove(HISTORY_FILE);
                nextSequenceId = 1;
                sendBLE("CLEAR:OK");
                Serial.println("[LittleFS] History cleared by phone command.");
            }
        }
    }
};

// -------- SETUP --------
void setup() {
    Serial.begin(115200);
    delay(500);

    analogReadResolution(12);

    ds18b20.begin();
    dht.begin();

    Serial.println("\n==========================================");
    Serial.println("  GrainGuardian 2.0 - Edge Sensor Engine  ");
    Serial.println("==========================================");
    Serial.print("DS18B20 1-Wire Devices Found: ");
    Serial.println(ds18b20.getDeviceCount());

    initStorage();

    // Initialize BLE
    BLEDevice::init("GrainGuardian");
    BLEServer *server = BLEDevice::createServer();
    server->setCallbacks(new GrainGuardianCallbacks());

    BLEService *service = server->createService(SERVICE_UUID);

    sensorCharacteristic = service->createCharacteristic(
        CHARACTERISTIC_UUID,
        BLECharacteristic::PROPERTY_READ |
        BLECharacteristic::PROPERTY_NOTIFY |
        BLECharacteristic::PROPERTY_WRITE
    );

    sensorCharacteristic->setCallbacks(new CommandCallbacks());
    sensorCharacteristic->addDescriptor(new BLE2902());
    sensorCharacteristic->setValue("GrainGuardian Ready");

    service->start();

    BLEAdvertising *advertising = BLEDevice::getAdvertising();
    advertising->addServiceUUID(SERVICE_UUID);
    advertising->setScanResponse(true);
    BLEDevice::startAdvertising();

    Serial.println("[BLE] GrainGuardian advertising initialized.");
}

// -------- MAIN EXECUTION LOOP --------
void loop() {
    unsigned long now = millis();

    // Restart advertising if disconnected
    if (!deviceConnected && oldDeviceConnected) {
        delay(500);
        BLEDevice::startAdvertising();
        Serial.println("[BLE] Restarted advertising after disconnect.");
        oldDeviceConnected = deviceConnected;
    }
    if (deviceConnected && !oldDeviceConnected) {
        oldDeviceConnected = deviceConnected;
    }

    // 1. Read Sensors
    ds18b20.requestTemperatures();
    float t1 = ds18b20.getTempCByIndex(0);
    float t2 = ds18b20.getTempCByIndex(1);
    float t3 = ds18b20.getTempCByIndex(2);

    float airTemp = dht.readTemperature();
    float humidity = dht.readHumidity();

    int moistureRaw = analogRead(MOISTURE_PIN);
    int moistureMV = analogReadMilliVolts(MOISTURE_PIN);

    // Fault flags
    uint8_t flags = 0;
    if (t1 != DEVICE_DISCONNECTED_C) flags |= (1 << 0);
    if (t2 != DEVICE_DISCONNECTED_C) flags |= (1 << 1);
    if (t3 != DEVICE_DISCONNECTED_C) flags |= (1 << 2);
    if (moistureMV > 150 && moistureMV < 3200) flags |= (1 << 3);
    if (!isnan(humidity)) flags |= (1 << 4);

    // 2. Periodic Autonomous Flash Logging (Every 5 Minutes)
    if (now - lastLogTime >= LOG_INTERVAL_MS || lastLogTime == 0) {
        lastLogTime = now;

        GrainRecord rec;
        rec.seq_id = nextSequenceId++;
        rec.uptime_sec = (uint32_t)(now / 1000);
        rec.t1_centic = (t1 == DEVICE_DISCONNECTED_C) ? -12700 : (int16_t)(t1 * 100);
        rec.t2_centic = (t2 == DEVICE_DISCONNECTED_C) ? -12700 : (int16_t)(t2 * 100);
        rec.t3_centic = (t3 == DEVICE_DISCONNECTED_C) ? -12700 : (int16_t)(t3 * 100);
        rec.moisture_mv = (uint16_t)moistureMV;
        rec.humidity = isnan(humidity) ? 65 : (uint8_t)humidity;
        rec.status_flags = flags;

        appendRecord(rec);
    }

    // 3. Live Stream to Android App (Every 2 Seconds when connected)
    if (deviceConnected && (now - lastLiveTime >= LIVE_INTERVAL_MS)) {
        lastLiveTime = now;

        sendBLE(t1 == DEVICE_DISCONNECTED_C ? "T1=ERR" : "T1=" + String(t1, 2));
        sendBLE(t2 == DEVICE_DISCONNECTED_C ? "T2=ERR" : "T2=" + String(t2, 2));
        sendBLE(t3 == DEVICE_DISCONNECTED_C ? "T3=ERR" : "T3=" + String(t3, 2));

        if (!isnan(airTemp)) sendBLE("AIR=" + String(airTemp, 1));
        if (!isnan(humidity)) sendBLE("HUM=" + String(humidity, 1));

        sendBLE("MR=" + String(moistureRaw));
        sendBLE("MV=" + String(moistureMV));
    }

    delay(200);
}
