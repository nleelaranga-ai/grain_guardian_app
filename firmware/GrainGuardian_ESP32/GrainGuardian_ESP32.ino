#include <OneWire.h>
#include <DallasTemperature.h>
#include <DHT.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
#include <FS.h>
#include <LittleFS.h>

// =========================================================================
// GRAINGUARDIAN ESP32 INDUSTRIAL EDGE FIRMWARE v2.0
// Smart Paddy Post-Harvest Readiness & Storage Health Intelligence System
// Multi-Depth Thermal Stratification, Capacitive Moisture, OLED & LittleFS
// =========================================================================

// -------- HARDWARE PIN CONFIGURATION --------
#define DS18B20_PIN     4   // 1-Wire bus with external 4.7k ohm pull-up to 3.3V
#define MOISTURE_PIN    34  // SEN0193 Capacitive Moisture Sensor (ADC1_CH6 exclusively)
#define DHT_PIN         27  // DHT11 Ambient Temperature & Headspace Humidity
#define DHT_TYPE        DHT11

// I2C Display Pins (Standard ESP32 hardware I2C)
#define I2C_SDA_PIN     21  // SSD1306 0.96" OLED Serial Data
#define I2C_SCL_PIN     22  // SSD1306 0.96" OLED Serial Clock
#define OLED_ADDR       0x3C
#define SCREEN_WIDTH    128
#define SCREEN_HEIGHT   64

// Local Alert Actuators (Tri-Color LEDs & Piezo Buzzer)
#define LED_SAFE_PIN    18  // Green LED: Moisture <= 13.5% & TGI <= 2.0C (Safe)
#define LED_WARN_PIN    19  // Yellow LED: Moisture 13.6-14.4% or TGI 2.0-2.9C (Warning)
#define LED_DANGER_PIN  23  // Red LED: Moisture >= 14.5% or TGI >= 3.0C or Hotspot (Critical)
#define BUZZER_PIN      25  // Active Piezo Buzzer (driven via 2N2222 NPN transistor)

// -------- BLE SERVICE & CHARACTERISTIC UUIDs --------
#define SERVICE_UUID        "12345678-1234-1234-1234-1234567890AB"
#define CHARACTERISTIC_UUID "12345678-1234-1234-1234-1234567890AC"

// -------- LOGGING & TELEMETRY TIMINGS --------
#define HISTORY_FILE        "/grain_history.bin"
#define LOG_INTERVAL_MS     300000UL  // 5 minutes autonomous flash logging
#define LIVE_INTERVAL_MS    2000UL    // 2 seconds live BLE stream interval
#define OLED_REFRESH_MS     1000UL    // 1 second OLED display refresh
#define MAX_HISTORY_RECORDS 30000UL   // 30,000 records * 18 bytes ≈ 540 KB (FIFO quota)

#define MAX_DS18B20_PROBES  3

// Fixed 18-byte packed binary structure for LittleFS Flash Persistence
struct __attribute__((packed)) GrainRecord {
    uint32_t seq_id;      // Monotonically increasing sequence number
    uint32_t uptime_sec;  // Seconds elapsed since boot
    int16_t  t1_centic;   // Temperature centi-Celsius (-12700 for disconnected)
    int16_t  t2_centic;   // Temperature centi-Celsius
    int16_t  t3_centic;   // Temperature centi-Celsius
    uint16_t moisture_mv; // Analog millivolts from ADC1 (0 - 3300 mV)
    uint8_t  humidity;    // Headspace relative humidity (0 - 100% RH)
    uint8_t  status_flags;// Bit0: T1_OK, Bit1: T2_OK, Bit2: T3_OK, Bit3: M_OK, Bit4: DHT_OK, Bit5: OLED_OK, Bit6: HOTSPOT
};

// System alert state for local actuation
enum SystemAlertLevel {
    LEVEL_SAFE = 0,
    LEVEL_WARNING = 1,
    LEVEL_DANGER = 2
};

// -------- HARDWARE OBJECTS & GLOBAL STATE --------
OneWire oneWire(DS18B20_PIN);
DallasTemperature ds18b20(&oneWire);
DHT dht(DHT_PIN, DHT_TYPE);
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, -1);

BLECharacteristic *sensorCharacteristic = nullptr;
bool deviceConnected = false;
bool oldDeviceConnected = false;
bool littleFsReady = false;
bool oledAvailable = false;

uint32_t nextSequenceId = 1;
unsigned long lastLogTime = 0;
unsigned long lastLiveTime = 0;
unsigned long lastOledTime = 0;

uint8_t discoveredProbeCount = 0;
DeviceAddress probeAddresses[MAX_DS18B20_PROBES];

// Live telemetry state cache
float lastT1 = DEVICE_DISCONNECTED_C;
float lastT2 = DEVICE_DISCONNECTED_C;
float lastT3 = DEVICE_DISCONNECTED_C;
float lastAirTemp = NAN;
float lastHumidity = NAN;
int lastMoistureRaw = 0;
int lastMoistureMV = 0;
float lastMoisturePct = 0.0f;
SystemAlertLevel currentAlertLevel = LEVEL_SAFE;

// -------- FORWARD DECLARATIONS --------
void sendBLE(String message);
void syncHistoryOverBle();
void appendRecord(const GrainRecord &rec);
void maintainStorageQuota();
int readMoistureMilliVolts();
float convertMvToMoisturePct(int mv);
SystemAlertLevel evaluateAlertLevel(float moisture, float t1, float t2, float t3);
void updateActuators(SystemAlertLevel level, unsigned long now);
void updateOledDisplay(float m, float t1, float t2, float t3, float air, float hum, SystemAlertLevel level);

// -------- BLE CALLBACKS & PROTOCOL ENGINE --------
class GrainGuardianCallbacks : public BLEServerCallbacks {
    void onConnect(BLEServer *server) override {
        deviceConnected = true;
        Serial.println("[BLE] GrainGuardian Mobile Client Connected!");
    }

    void onDisconnect(BLEServer *server) override {
        deviceConnected = false;
        Serial.println("[BLE] Mobile Client Disconnected. Maintaining Autonomous Edge Logging.");
    }
};

void sendBLE(String message) {
    if (deviceConnected && sensorCharacteristic != nullptr) {
        sensorCharacteristic->setValue(message.c_str());
        sensorCharacteristic->notify();
        delay(40); // Standard BLE packet inter-frame spacing
    }
}

// Inbound Command Handler (Android -> ESP32)
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
            String info = "INFO:VER=2.0;RECS=" + String(recCount) +
                          ";UP=" + String(millis() / 1000) +
                          ";PROBES=" + String(discoveredProbeCount) +
                          ";OLED=" + (oledAvailable ? "1" : "0");
            sendBLE(info);
        }
        else if (rxValue == "CMD:SYNC") {
            syncHistoryOverBle();
        }
        else if (rxValue == "CMD:CLEAR_HISTORY") {
            if (littleFsReady) {
                LittleFS.remove(HISTORY_FILE);
                nextSequenceId = 1;
                sendBLE("CLEAR:OK");
                Serial.println("[LittleFS] Flash history erased by client command.");
            }
        }
        else if (rxValue == "CMD:PING") {
            sendBLE("PONG");
        }
        else if (rxValue == "CMD:BEEP") {
            digitalWrite(BUZZER_PIN, HIGH);
            delay(150);
            digitalWrite(BUZZER_PIN, LOW);
            sendBLE("BEEP:OK");
        }
    }
};

// Stream buffered flash records over BLE to Android client
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
    Serial.printf("[BLE Sync] Complete. %u / %u records successfully transmitted.\n", sentCount, totalRecords);
}

// -------- LITTLEFS PERSISTENCE ENGINE --------
void initStorage() {
    if (!LittleFS.begin(true)) {
        Serial.println("[LittleFS] Partition Mount Failed! Autonomous logging disabled.");
        littleFsReady = false;
        return;
    }
    littleFsReady = true;
    Serial.println("[LittleFS] Partition Mounted Successfully.");

    // Enumerate existing records to reconstitute monotonic nextSequenceId
    if (LittleFS.exists(HISTORY_FILE)) {
        File file = LittleFS.open(HISTORY_FILE, FILE_READ);
        if (file) {
            size_t count = file.size() / sizeof(GrainRecord);
            Serial.printf("[LittleFS] Found %u existing offline records (%u bytes).\n", count, file.size());
            if (count > 0) {
                file.seek((count - 1) * sizeof(GrainRecord));
                GrainRecord lastRec;
                if (file.read((uint8_t*)&lastRec, sizeof(GrainRecord)) == sizeof(GrainRecord)) {
                    nextSequenceId = lastRec.seq_id + 1;
                    Serial.printf("[LittleFS] Resuming sequence numbering from #%u\n", nextSequenceId);
                }
            }
            file.close();
        }
    }
}

void appendRecord(const GrainRecord &rec) {
    if (!littleFsReady) return;
    maintainStorageQuota();

    File file = LittleFS.open(HISTORY_FILE, FILE_APPEND);
    if (!file) {
        Serial.println("[LittleFS] Error opening history file for append!");
        return;
    }
    file.write((const uint8_t*)&rec, sizeof(GrainRecord));
    file.close();

    Serial.printf("[LittleFS] Appended Record #%u (MV=%u mV, T1=%.1fC, T2=%.1fC, T3=%.1fC, Flags=0x%02X)\n",
        rec.seq_id,
        rec.moisture_mv,
        rec.t1_centic == -12700 ? -127.0f : rec.t1_centic / 100.0f,
        rec.t2_centic == -12700 ? -127.0f : rec.t2_centic / 100.0f,
        rec.t3_centic == -12700 ? -127.0f : rec.t3_centic / 100.0f,
        rec.status_flags
    );
}

// Circular ring-buffer compaction when LittleFS file approaches storage quota
void maintainStorageQuota() {
    if (!littleFsReady || !LittleFS.exists(HISTORY_FILE)) return;
    File file = LittleFS.open(HISTORY_FILE, FILE_READ);
    if (!file) return;
    size_t count = file.size() / sizeof(GrainRecord);
    file.close();

    if (count > MAX_HISTORY_RECORDS) {
        Serial.printf("[LittleFS] Quota exceeded (%u records). Compacting oldest 10,000 records...\n", count);
        File src = LittleFS.open(HISTORY_FILE, FILE_READ);
        File dst = LittleFS.open("/temp_compact.bin", FILE_WRITE);
        if (src && dst) {
            size_t skipRecords = 10000;
            src.seek(skipRecords * sizeof(GrainRecord));
            GrainRecord tempRec;
            while (src.read((uint8_t*)&tempRec, sizeof(GrainRecord)) == sizeof(GrainRecord)) {
                dst.write((const uint8_t*)&tempRec, sizeof(GrainRecord));
            }
            src.close();
            dst.close();
            LittleFS.remove(HISTORY_FILE);
            LittleFS.rename("/temp_compact.bin", HISTORY_FILE);
            Serial.println("[LittleFS] Compaction completed successfully.");
        } else {
            if (src) src.close();
            if (dst) dst.close();
        }
    }
}

// -------- SENSOR ACQUISITION & CALIBRATION --------
// 16-sample oversampled analog reading with peak/trough spike rejection
int readMoistureMilliVolts() {
    const int SAMPLES = 16;
    int minVal = 4096;
    int maxVal = -1;
    long sum = 0;

    for (int i = 0; i < SAMPLES; i++) {
        int mv = analogReadMilliVolts(MOISTURE_PIN);
        sum += mv;
        if (mv < minVal) minVal = mv;
        if (mv > maxVal) maxVal = mv;
        delay(3);
    }
    // Reject highest and lowest outliers
    sum -= (minVal + maxVal);
    int avgMv = sum / (SAMPLES - 2);
    return avgMv;
}

// SEN0193 Capacitive Moisture Sensor Calibration for Rough Paddy
// Baseline: ~2100 mV = 13.5% (Safe baseline), ~2600 mV = 10.0%, ~1600 mV = 18.0%
float convertMvToMoisturePct(int mv) {
    if (mv < 150 || mv > 3250) {
        return 0.0f; // Disconnected or shorted probe fault
    }
    float mc = 13.5f + (2100.0f - (float)mv) / 500.0f * 4.5f;
    return constrain(mc, 8.0f, 35.0f);
}

// Dynamic multi-probe 1-Wire discovery on GPIO 4
void scanOneWireProbes() {
    ds18b20.begin();
    discoveredProbeCount = ds18b20.getDeviceCount();
    Serial.printf("[1-Wire] Discovered %u DS18B20 digital probes on GPIO %d\n", discoveredProbeCount, DS18B20_PIN);

    for (uint8_t i = 0; i < discoveredProbeCount && i < MAX_DS18B20_PROBES; i++) {
        if (ds18b20.getAddress(probeAddresses[i], i)) {
            Serial.printf("  -> Probe #%d [Depth Level %d] ROM: ", i + 1, i + 1);
            for (uint8_t b = 0; b < 8; b++) {
                if (probeAddresses[i][b] < 16) Serial.print("0");
                Serial.print(probeAddresses[i][b], HEX);
                if (b < 7) Serial.print(":");
            }
            Serial.println();
            ds18b20.setResolution(probeAddresses[i], 12); // 12-bit precision (0.0625°C)
        }
    }
}

// Filtered DS18B20 temperature read with power-on reset 85.0C scratchpad rejection
float readFilteredTemp(uint8_t probeIndex) {
    if (probeIndex >= discoveredProbeCount && discoveredProbeCount > 0) {
        return DEVICE_DISCONNECTED_C;
    }

    float t = (discoveredProbeCount > 0) 
        ? ds18b20.getTempC(probeAddresses[probeIndex]) 
        : ds18b20.getTempCByIndex(probeIndex);

    // Scratchpad power-on default is 85.0C; re-request if detected
    if (t == 85.0f) {
        delay(100);
        ds18b20.requestTemperatures();
        t = (discoveredProbeCount > 0) 
            ? ds18b20.getTempC(probeAddresses[probeIndex]) 
            : ds18b20.getTempCByIndex(probeIndex);
    }
    return t;
}

// Local Risk & Alert Evaluation Engine
SystemAlertLevel evaluateAlertLevel(float moisture, float t1, float t2, float t3) {
    float maxT = -100.0f;
    float minT = 100.0f;
    int validCount = 0;

    if (t1 != DEVICE_DISCONNECTED_C) {
        if (t1 > maxT) maxT = t1;
        if (t1 < minT) minT = t1;
        validCount++;
    }
    if (t2 != DEVICE_DISCONNECTED_C) {
        if (t2 > maxT) maxT = t2;
        if (t2 < minT) minT = t2;
        validCount++;
    }
    if (t3 != DEVICE_DISCONNECTED_C) {
        if (t3 > maxT) maxT = t3;
        if (t3 < minT) minT = t3;
        validCount++;
    }

    float tgi = (validCount >= 2) ? (maxT - minT) : 0.0f;

    // Critical Danger: Hotspot detected (>35.0C or gradient >3.0C) or severe wet grain (>14.5%)
    if ((maxT >= 35.0f && validCount > 0) || tgi >= 3.0f || moisture >= 14.5f) {
        return LEVEL_DANGER;
    }

    // Warning: Moisture in 13.6 - 14.4% range or temperature gradient 2.0 - 2.9C
    if (moisture > 13.5f || tgi >= 2.0f || (maxT >= 32.0f && validCount > 0)) {
        return LEVEL_WARNING;
    }

    return LEVEL_SAFE;
}

// Actuator control: Tri-color LEDs and intermittent buzzer alarm
void updateActuators(SystemAlertLevel level, unsigned long now) {
    switch (level) {
        case LEVEL_SAFE:
            digitalWrite(LED_SAFE_PIN, HIGH);
            digitalWrite(LED_WARN_PIN, LOW);
            digitalWrite(LED_DANGER_PIN, LOW);
            digitalWrite(BUZZER_PIN, LOW);
            break;

        case LEVEL_WARNING:
            digitalWrite(LED_SAFE_PIN, LOW);
            digitalWrite(LED_WARN_PIN, HIGH);
            digitalWrite(LED_DANGER_PIN, LOW);
            digitalWrite(BUZZER_PIN, LOW);
            break;

        case LEVEL_DANGER:
            digitalWrite(LED_SAFE_PIN, LOW);
            digitalWrite(LED_WARN_PIN, LOW);
            digitalWrite(LED_DANGER_PIN, HIGH);
            // Non-blocking 100ms chirp every 15 seconds to conserve battery while alerting warehouse staff
            if ((now % 15000UL) < 100UL) {
                digitalWrite(BUZZER_PIN, HIGH);
            } else {
                digitalWrite(BUZZER_PIN, LOW);
            }
            break;
    }
}

// -------- OLED DISPLAY ENGINE --------
void initOledDisplay() {
    Wire.begin(I2C_SDA_PIN, I2C_SCL_PIN);
    Wire.beginTransmission(OLED_ADDR);
    if (Wire.endTransmission() == 0) {
        if (display.begin(SSD1306_SWITCHCAPVCC, OLED_ADDR)) {
            oledAvailable = true;
            display.clearDisplay();
            display.setTextColor(SSD1306_WHITE);
            display.setTextSize(1);
            display.setCursor(8, 12);
            display.println("GrainGuardian 2.0");
            display.setCursor(8, 28);
            display.println("Smart Paddy Probe");
            display.setCursor(8, 44);
            display.println("Sensors Initialized");
            display.display();
            Serial.println("[OLED] SSD1306 128x64 display online at 0x3C.");
        }
    } else {
        Serial.println("[OLED] Display not detected at 0x3C (skipping OLED UI gracefully).");
    }
}

void updateOledDisplay(float m, float t1, float t2, float t3, float air, float hum, SystemAlertLevel level) {
    if (!oledAvailable) return;

    display.clearDisplay();
    display.setTextSize(1);
    display.setTextColor(SSD1306_WHITE);

    // Header & BLE link status
    display.setCursor(0, 0);
    display.print("GrainGuardian");
    display.setCursor(82, 0);
    display.print(deviceConnected ? "[BLE:CON]" : "[BLE:ADV]");
    display.drawLine(0, 10, 127, 10, SSD1306_WHITE);

    // Moisture & Voltage
    display.setCursor(0, 13);
    display.print("Moist: ");
    if (m > 0.0f) {
        display.print(m, 1);
        display.print("%");
    } else {
        display.print("ERR");
    }
    display.setCursor(75, 13);
    display.print("(" + String(lastMoistureMV) + "mV)");

    // 3-Depth Temperatures
    display.setCursor(0, 25);
    display.print("T1:");
    display.print(t1 == DEVICE_DISCONNECTED_C ? "ERR" : String(t1, 1));
    display.print(" T2:");
    display.print(t2 == DEVICE_DISCONNECTED_C ? "ERR" : String(t2, 1));
    display.print(" T3:");
    display.print(t3 == DEVICE_DISCONNECTED_C ? "ERR" : String(t3, 1));

    // Ambient Headspace (Air & Humidity)
    display.setCursor(0, 37);
    display.print("Air:");
    display.print(isnan(air) ? "ERR" : String(air, 1));
    display.print("C  Hum:");
    display.print(isnan(hum) ? "ERR" : String(hum, 0));
    display.print("%");

    // Divider
    display.drawLine(0, 48, 127, 48, SSD1306_WHITE);

    // Alert Status Banner
    display.setCursor(0, 52);
    switch (level) {
        case LEVEL_SAFE:
            display.print("STATUS: SAFE (NORMAL)");
            break;
        case LEVEL_WARNING:
            display.print("STATUS: WARN (MONITOR)");
            break;
        case LEVEL_DANGER:
            display.print("STATUS: DANGER HOTSPOT");
            break;
    }

    display.display();
}

// -------- SETUP --------
void setup() {
    Serial.begin(115200);
    delay(500);

    Serial.println("\n=======================================================");
    Serial.println("  GrainGuardian 2.0 — Edge Sensor & BLE Firmare        ");
    Serial.println("=======================================================");

    // Pin Modes
    pinMode(LED_SAFE_PIN, OUTPUT);
    pinMode(LED_WARN_PIN, OUTPUT);
    pinMode(LED_DANGER_PIN, OUTPUT);
    pinMode(BUZZER_PIN, OUTPUT);

    // Initial LED sequence: all ON for 300ms self-test
    digitalWrite(LED_SAFE_PIN, HIGH);
    digitalWrite(LED_WARN_PIN, HIGH);
    digitalWrite(LED_DANGER_PIN, HIGH);
    digitalWrite(BUZZER_PIN, HIGH);
    delay(100);
    digitalWrite(BUZZER_PIN, LOW);
    delay(200);
    digitalWrite(LED_SAFE_PIN, LOW);
    digitalWrite(LED_WARN_PIN, LOW);
    digitalWrite(LED_DANGER_PIN, LOW);

    // ADC Configuration: 12-bit resolution with 11dB attenuation (0 - 3.1V range)
    analogReadResolution(12);
    analogSetPinAttenuation(MOISTURE_PIN, ADC_11db);

    // Sensor Inits
    scanOneWireProbes();
    dht.begin();
    initStorage();
    initOledDisplay();

    // BLE GATT Server Initialization
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

    Serial.println("[BLE] GATT Server active. Advertising 'GrainGuardian'.");
    Serial.println("=======================================================\n");
}

// -------- MAIN EXECUTION LOOP --------
void loop() {
    unsigned long now = millis();

    // Re-arm advertising if client disconnected
    if (!deviceConnected && oldDeviceConnected) {
        delay(500);
        BLEDevice::startAdvertising();
        Serial.println("[BLE] Re-armed advertising after client disconnect.");
        oldDeviceConnected = deviceConnected;
    }
    if (deviceConnected && !oldDeviceConnected) {
        oldDeviceConnected = deviceConnected;
    }

    // 1. Acquire Sensor Readings
    ds18b20.requestTemperatures();
    lastT1 = readFilteredTemp(0);
    lastT2 = readFilteredTemp(1);
    lastT3 = readFilteredTemp(2);

    lastAirTemp = dht.readTemperature();
    lastHumidity = dht.readHumidity();

    lastMoistureRaw = analogRead(MOISTURE_PIN);
    lastMoistureMV = readMoistureMilliVolts();
    lastMoisturePct = convertMvToMoisturePct(lastMoistureMV);

    // 2. Compute Health Flags
    uint8_t flags = 0;
    if (lastT1 != DEVICE_DISCONNECTED_C) flags |= (1 << 0);
    if (lastT2 != DEVICE_DISCONNECTED_C) flags |= (1 << 1);
    if (lastT3 != DEVICE_DISCONNECTED_C) flags |= (1 << 2);
    if (lastMoisturePct > 0.0f)          flags |= (1 << 3);
    if (!isnan(lastHumidity))            flags |= (1 << 4);
    if (oledAvailable)                   flags |= (1 << 5);

    // 3. Evaluate Risk Level and Drive Actuators
    currentAlertLevel = evaluateAlertLevel(lastMoisturePct, lastT1, lastT2, lastT3);
    if (currentAlertLevel == LEVEL_DANGER) flags |= (1 << 6); // Bit6 = HOTSPOT
    updateActuators(currentAlertLevel, now);

    // 4. Update OLED Display (Every 1000 ms)
    if (oledAvailable && (now - lastOledTime >= OLED_REFRESH_MS || lastOledTime == 0)) {
        lastOledTime = now;
        updateOledDisplay(lastMoisturePct, lastT1, lastT2, lastT3, lastAirTemp, lastHumidity, currentAlertLevel);
    }

    // 5. Periodic Autonomous Flash Logging (Every 5 Minutes in Autonomous Mode)
    if (now - lastLogTime >= LOG_INTERVAL_MS || lastLogTime == 0) {
        lastLogTime = now;

        GrainRecord rec;
        rec.seq_id = nextSequenceId++;
        rec.uptime_sec = (uint32_t)(now / 1000UL);
        rec.t1_centic = (lastT1 == DEVICE_DISCONNECTED_C) ? -12700 : (int16_t)(lastT1 * 100);
        rec.t2_centic = (lastT2 == DEVICE_DISCONNECTED_C) ? -12700 : (int16_t)(lastT2 * 100);
        rec.t3_centic = (lastT3 == DEVICE_DISCONNECTED_C) ? -12700 : (int16_t)(lastT3 * 100);
        rec.moisture_mv = (uint16_t)lastMoistureMV;
        rec.humidity = isnan(lastHumidity) ? 65 : (uint8_t)lastHumidity;
        rec.status_flags = flags;

        appendRecord(rec);
    }

    // 6. Real-Time Telemetry Stream to Connected Android Client (Every 2 Seconds)
    if (deviceConnected && (now - lastLiveTime >= LIVE_INTERVAL_MS)) {
        lastLiveTime = now;

        sendBLE(lastT1 == DEVICE_DISCONNECTED_C ? "T1=ERR" : "T1=" + String(lastT1, 2));
        sendBLE(lastT2 == DEVICE_DISCONNECTED_C ? "T2=ERR" : "T2=" + String(lastT2, 2));
        sendBLE(lastT3 == DEVICE_DISCONNECTED_C ? "T3=ERR" : "T3=" + String(lastT3, 2));

        if (!isnan(lastAirTemp))  sendBLE("AIR=" + String(lastAirTemp, 1));
        if (!isnan(lastHumidity)) sendBLE("HUM=" + String(lastHumidity, 1));

        sendBLE("MR=" + String(lastMoistureRaw));
        sendBLE("MV=" + String(lastMoistureMV));
        if (lastMoisturePct > 0.0f) sendBLE("M=" + String(lastMoisturePct, 1));
    }

    delay(200);
}
