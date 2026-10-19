# 05 — IOT FIRMWARE & SENSOR SUBSYSTEM AUDIT

**Scope:** Analysis of ESP32 firmware implementation, sensor pin assignments, hardware libraries, communication interfaces, and physical verification boundaries.

---

## 1. Firmware Source Code & Detected Libraries

The primary firmware implementation is located at:  
[`firmware/GrainGuardian_ESP32/GrainGuardian_ESP32.ino`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/firmware/GrainGuardian_ESP32/GrainGuardian_ESP32.ino)

### Detected Arduino Libraries
```cpp
#include <OneWire.h>            // Maxim 1-Wire protocol driver (Paul Stoffregen)
#include <DallasTemperature.h>  // DS18B20 digital temperature library (Miles Burton)
#include <DHT.h>                // Single-bus DHT temperature & humidity (Adafruit)
#include <BLEDevice.h>          // Espressif ESP32 native BLE stack
#include <BLEServer.h>          // ESP32 BLE GATT server
#include <BLEUtils.h>           // ESP32 BLE helper utilities
#include <BLE2902.h>            // Client Characteristic Configuration Descriptor (0x2902)
```

---

## 2. Hardware Pin Assignments & Electrical Constraints

| Component | Function / Variable | Hardware Pin | Subsystem / ADC Channel | Electrical Constraints & Verification Notes |
| :--- | :--- | :---: | :---: | :--- |
| **Maxim DS18B20 (x3)** | 3-Depth Digital Temperatures | `GPIO 4` | Digital 1-Wire Bus | Requires external **4.7 kΩ pull-up resistor** to 3.3V. All 3 probes share this single bus in multi-drop topology. |
| **SEN0193 V2.0** | Grain Moisture (Analog Output) | `GPIO 34` | **ADC1_CH6** | **Critical Silicon Rule:** `GPIO 34` belongs to ADC1. ADC1 is completely independent of the BLE radio. (ADC2 pins fail when BLE is active). |
| **DHT11** | Ambient Temperature & Humidity | `GPIO 27` | Digital Single-Bus | Internal or external 10 kΩ pull-up required. Used for ambient warehouse conditions. |
| **Buzzer / LED** | Audio/Visual Alert | *Unassigned in user sketch* | Digital Output | Documented in wiring guide for `GPIO 26` / `GPIO 25`, but omitted from the basic user sketch. |
| **OLED (SSD1306)** | Local 0.96-inch Display | *Unassigned in user sketch* | I2C (`GPIO 21` SDA, `GPIO 22` SCL) | Mentioned in BOM and circuit guide; not included in user's basic sketch. |

---

## 3. Communication Protocol & Telemetry Framing

### BLE Profile Specification
- **Device Advertising Name:** `"GrainGuardian"`
- **GATT Service UUID:** `12345678-1234-1234-1234-1234567890AB`
- **GATT Characteristic UUID:** `12345678-1234-1234-1234-1234567890AC`
- **Characteristic Properties:** `BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_NOTIFY`
- **Descriptor:** `BLE2902` (UUID: `00002902-0000-1000-8000-00805f9b34fb`)

### Telemetry Packet Transmission Sequence
When a client connects, the firmware executes a 2000 ms sampling loop, transmitting 7 individual notifications spaced 60 ms apart:

```
[ESP32] -> sendBLE("T1=25.38")
[ESP32] -> sendBLE("T2=25.56")
[ESP32] -> sendBLE("T3=25.94")
[ESP32] -> sendBLE("AIR=24.9")
[ESP32] -> sendBLE("HUM=64.4")
[ESP32] -> sendBLE("MR=2368")
[ESP32] -> sendBLE("MV=2110")
```

### Safety & Error Handling in Firmware
- If a DS18B20 is disconnected or fails checksum, `getTempCByIndex()` returns `-127.00^\circ\text{C}` (`DEVICE_DISCONNECTED_C`).
  Firmware converts this to: `sendBLE("T1=ERR")`.
- If the DHT11 fails to read, `isnan(airTemp)` is caught, outputting: `sendBLE("AIR=ERR")`.
- If BLE disconnects, `onDisconnect()` invokes `BLEDevice::startAdvertising()`, allowing the Android app to reconnect seamlessly.

---

## 4. Hardware Verification & Ground-Truth Evidence

### Verified via Direct User Serial Evidence
The following live terminal output was provided directly by the user from their running ESP32 prototype:
```
--- GrainGuardian Sensor Data ---
T1: 25.38 C
T2: 25.56 C
T3: 25.94 C
Air: 24.9 C
Humidity: 64.4 % RH
Moisture raw: 2368
Moisture voltage: 2110 mV
```

### Auditor Physical Verification Boundary
While firmware source code and serial outputs confirm the ESP32 code functions:
1. **Physical Sensor Calibration:** The SEN0193 voltage-to-moisture mapping ($2110\text{ mV} \rightarrow 13.4\%$) is based on an empirical model. It has **NOT** been certified against laboratory air-oven reference samples (IS 4333 / ISO 712) across different bulk compaction densities.
2. **Sensor Indexing:** `ds18b20.getTempCByIndex(0..2)` assigns sensor depths based on the factory 64-bit ROM addresses sorted ascendingly. If probes are wired without matching ROM addresses to probe markings, $T_1$ (Top) and $T_3$ (Bottom) may be physically inverted.
3. **Autonomous Local Logging:** The current sketch only transmits live BLE notifications. It **does not** log to LittleFS flash memory when disconnected. Continuous Mode B storage logging requires future firmware extension.
