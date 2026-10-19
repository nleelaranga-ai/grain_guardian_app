# GrainGuardian 2.0 — ESP32 Hardware Wiring & Sensor Bus Specification

This technical specification details the complete physical wiring, electrical pinouts, sensor interfaces, and flashing instructions for the **GrainGuardian Smart Paddy Storage & Post-Harvest Monitoring Probe**.

---

## 1. Master GPIO Pinout Table

All pin assignments are rigorously assigned according to the ESP32 silicon errata and ADC architecture:
- **`GPIO 34` (SEN0193)** is assigned to **ADC1 Channel 6**. (ADC1 channels remain 100% operational when the Bluetooth/Wi-Fi radio is active. Never use ADC2 pins when BLE is enabled).
- **`GPIO 4` (DS18B20 1-Wire)** is pulled up externally to 3.3V with a 4.7 kΩ resistor.
- **`GPIO 21 & 22` (I2C Bus)** interface with the SSD1306 OLED display.
- **`GPIO 25` (Buzzer)** is driven via a 2N2222 NPN transistor to isolate inductive back-EMF from the ESP32.

| Subsystem / Peripheral | Sensor / Actuator Model | ESP32 Hardware Pin | Protocol / Channel | Voltage Rail | Current Draw (Typ.) | Essential External Passive Components |
| :--- | :--- | :---: | :---: | :---: | :---: | :--- |
| **Top Grain Temperature ($T_1$)** | Maxim DS18B20 (Waterproof) | `GPIO 4` | 1-Wire Digital Multi-Drop | `3.3V` | 1.5 mA | **4.7 kΩ pull-up resistor** between `DATA` & `3.3V` |
| **Middle Grain Temperature ($T_2$)** | Maxim DS18B20 (Waterproof) | `GPIO 4` | 1-Wire Digital Multi-Drop | `3.3V` | 1.5 mA | Shared bus with $T_1$ |
| **Bottom Grain Temperature ($T_3$)** | Maxim DS18B20 (Waterproof) | `GPIO 4` | 1-Wire Digital Multi-Drop | `3.3V` | 1.5 mA | Shared bus with $T_1$ |
| **Capacitive Grain Moisture** | DFRobot SEN0193 V2.0 | `GPIO 34` | **ADC1_CH6** (Analog Input) | `3.3V` | 5.0 mA | Bypass capacitor (0.1 µF ceramic across `VCC` & `GND`) |
| **Headspace Ambient Temp & RH** | Aosong DHT11 / DHT22 | `GPIO 27` | Single-Bus Digital | `3.3V` | 2.5 mA | Internal pull-up or external 10 kΩ pull-up to `3.3V` |
| **Local Graphical Display** | 0.96" SSD1306 Monochrome OLED | `GPIO 21` (SDA)<br>`GPIO 22` (SCL) | I2C (Address `0x3C`) | `3.3V` | 20 mA | None (internal pull-ups enabled in `Wire.h`) |
| **Normal / Safe Status LED** | 5mm Green LED | `GPIO 18` | Digital Output | `3.3V` | 8.0 mA | **220 Ω current-limiting resistor** in series |
| **Warning Status LED** | 5mm Yellow LED | `GPIO 19` | Digital Output | `3.3V` | 8.0 mA | **220 Ω current-limiting resistor** in series |
| **Danger / Hotspot Status LED** | 5mm Red LED | `GPIO 23` | Digital Output | `3.3V` | 8.0 mA | **220 Ω current-limiting resistor** in series |
| **Acoustic Warning Alarm** | 5V Active Piezo Buzzer | `GPIO 25` | Digital Output (PWM/Tone) | `5.0V` | 35 mA | **2N2222 NPN transistor**, **1 kΩ Base resistor**, **1N4148 flyback diode** |

---

## 2. Sensor Probe Shaft Assembly

```
                        [HEAD UNIT ENCLOSURE]
                   +-----------------------------+
                   |  ESP32 DevKit + TP4056 +    |
                   |  18650 Cell + OLED Display  |
                   |  Status LEDs + Piezo Buzzer |
                   +--------------+--------------+
                                  |
                                  | 25mm PVC / SS Shaft
                                  | (Perforated for thermal airflow)
                                  |
                   [0.3m - 0.5m]  |====> DS18B20 Probe #1 (T1: Surface / Top Layer)
                                  |
                                  |
                   [0.8m - 1.0m]  |====> DS18B20 Probe #2 (T2: Middle Stratum)
                                  |
                                  |
                   [1.3m - 1.5m]  |====> DS18B20 Probe #3 (T3: Floor / Bottom Stratum)
                                  |
                           (Tip)  \=====> SEN0193 Capacitive Moisture Sensor (Conical Tip)
```

1. **Shaft Material:** 25 mm OD schedule 40 PVC or stainless steel tubing, perforated with 3 mm holes around each sensor point.
2. **Moisture Potting:** SEN0193 electronics and cable junctions must be potted with neutral-cure electronic-grade silicone (RTV) to shield circuit components from grain dust and moisture condensation while leaving the capacitive copper trace plate exposed to the grain.
3. **Sensor Spacing:** Probes are positioned at 0.4 m, 0.9 m, and 1.4 m to detect vertical convective air currents and hotspot plumes.

---

## 3. Buzzer & Transistor Drive Circuit

> [!CAUTION]
> Never connect an active buzzer directly between an ESP32 GPIO pin and GND. Piezo buzzers draw 30–50 mA (exceeding the 12 mA safe GPIO limit) and induce voltage spikes that can crash the ESP32 CPU.

```
       +5V Rail (from Boost Converter / USB VBUS)
           |
           +---+
               |
             [ + ] Active Buzzer (5V)
             [ - ]
               |
               +-------+
               |       |
             [ |< ]  1N4148 Flyback Diode (Cathode to +5V, Anode to Collector)
               |       |
               +-------+
               |
           Collector (C)
  GPIO 25 ---[ 1 kΩ ]---> Base (B)  [2N2222 NPN Transistor]
           Emitter (E)
               |
              GND
```

---

## 4. SEN0193 Moisture Calibration Curve

The ESP32 firmware samples the capacitive moisture sensor on `GPIO 34` with 11dB attenuation and 16-sample spike filtering:

$$\text{Moisture \%} = 13.5 + \left(\frac{2100 - V_{\text{mV}}}{500}\right) \times 4.5$$

- **In Free Air (Dry):** $V_{\text{mV}} \approx 2800\text{–}3000\text{ mV} \longrightarrow 8.0\%$ (Clamped Minimum)
- **Safe Storage Baseline:** $V_{\text{mV}} = 2100\text{ mV} \longrightarrow 13.5\%$ (Ideal Indian Standard Paddy)
- **Wet Grain Threshold:** $V_{\text{mV}} = 1600\text{ mV} \longrightarrow 18.0\%$ (Requires Immediate Drying)
- **Saturated / Water (Fault):** $V_{\text{mV}} < 1200\text{ mV} \longrightarrow 22\text{–}32\%$ (Critical Hotspot / Spoiled Wet Grain)

---

## 5. Firmware Flashing Instructions

### Method A: Arduino IDE 2.x
1. Install **Arduino IDE 2.3+**.
2. Add ESP32 Board Package:
   - File $\rightarrow$ Preferences $\rightarrow$ Additional Board Manager URLs:
     `https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json`
   - Tools $\rightarrow$ Board $\rightarrow$ Boards Manager $\rightarrow$ Search **"esp32" by Espressif Systems** $\rightarrow$ Install version **2.0.14** or **3.0.x**.
3. Install Required Libraries via Library Manager (Tools $\rightarrow$ Manage Libraries):
   - `OneWire` by Paul Stoffregen
   - `DallasTemperature` by Miles Burton
   - `DHT sensor library` by Adafruit
   - `Adafruit Unified Sensor` by Adafruit
   - `Adafruit SSD1306` by Adafruit
   - `Adafruit GFX Library` by Adafruit
4. Board Configuration:
   - **Board:** "ESP32 Dev Module"
   - **Upload Speed:** 921600 (or 115200)
   - **CPU Frequency:** 240MHz (WiFi/BT)
   - **Flash Frequency:** 80MHz
   - **Partition Scheme:** "Default 4MB with ffat" or "Minimal SPIFFS (1.9MB APP with OTA/190KB SPIFFS)"
5. Open `firmware/GrainGuardian_ESP32/GrainGuardian_ESP32.ino`.
6. Select the COM Port of your ESP32.
7. Click **Upload**.

### Method B: PlatformIO (VS Code CLI)
1. Open the project root in VS Code with the PlatformIO extension.
2. In terminal, navigate to `firmware`:
   ```bash
   cd firmware
   pio run -t upload
   pio device monitor -b 115200
   ```

---

## 6. Serial Boot Verification Checklist

Upon successful flashing and power-up, open the Serial Monitor at **115200 baud**. You will observe:

```
=======================================================
  GrainGuardian 2.0 — Edge Sensor & BLE Firmware        
=======================================================
[1-Wire] Discovered 3 DS18B20 digital probes on GPIO 4
  -> Probe #1 [Depth Level 1] ROM: 28:AA:B4:C8:0B:00:00:D9
  -> Probe #2 [Depth Level 2] ROM: 28:61:64:12:4F:72:08:BE
  -> Probe #3 [Depth Level 3] ROM: 28:44:89:90:05:00:00:3C
[LittleFS] Partition Mounted Successfully.
[LittleFS] Found 0 existing offline records (0 bytes).
[LittleFS] Resuming sequence numbering from #1
[OLED] SSD1306 128x64 display online at 0x3C.
[BLE] GATT Server active. Advertising 'GrainGuardian'.
=======================================================
```

---

## 7. BLE Telemetry & Command Protocol Reference

- **Service UUID:** `12345678-1234-1234-1234-1234567890AB`
- **Characteristic UUID:** `12345678-1234-1234-1234-1234567890AC` (Notify + Read + Write)

### Commands Supported by Probe
| Command | Response | Action |
| :--- | :--- | :--- |
| `CMD:INFO\n` | `INFO:VER=2.0;RECS=n;UP=s;PROBES=3;OLED=1` | Returns device health, LittleFS record count, and probe census. |
| `CMD:SYNC\n` | `SYNC:START,TOTAL=n`<br>`HIST:SEQ=1,UP=...,T1=...,T2=...,T3=...,MV=...,H=...,F=...`<br>`SYNC:END,SENT=n` | Streams all autonomous offline records stored in LittleFS flash. |
| `CMD:CLEAR_HISTORY\n` | `CLEAR:OK` | Erases `/grain_history.bin` and resets sequence IDs. |
| `CMD:PING\n` | `PONG` | Verifies two-way BLE roundtrip latency. |
| `CMD:BEEP\n` | `BEEP:OK` | Sounds a 150 ms acoustic test chirp on GPIO 25. |
