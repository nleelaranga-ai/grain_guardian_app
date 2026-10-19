# GrainGuardian — Phase 8: College Review Defense Script & Hardware Poster

## 1. Ten-Minute Live Demonstration Script (For External Examiners & Project Review)

### Timeline & Stage-by-Stage Flow

```mermaid
flowchart LR
    A["00:00 - 01:30<br>Hardware Power-on & Self-Test"] --> B["01:30 - 03:00<br>BLE Pairing & Safe Baseline"]
    B --> C["03:00 - 05:30<br>Hotspot Induction & Buzzer Alarm"]
    C --> D["05:30 - 07:00<br>Drying Advisor & PDF Export"]
    D --> E["07:00 - 08:30<br>Cloud Sync to FastAPI Backend"]
    E --> F["08:30 - 10:00<br>Examiner Q&A & Viva Defense"]
```

---

### Step 1: Hardware Power-on & Self-Test (00:00 – 01:30)
1. **Action:** Connect ESP32 to power bank or USB port.
2. **Examiner Talking Point:**
   > *"Good morning, respected examiners. We begin by powering on the GrainGuardian ESP32 probe. Notice the SSD1306 OLED display automatically executes an I2C bus scan (0x3C), initializes the Dallas 1-Wire bus on GPIO 4, and dynamically enumerates all 3 DS18B20 temperature sensors across the probe depths. The green status LED on GPIO 18 indicates normal battery and sensor bus integrity."*
3. **Evidence:** OLED displays: `GrainGuardian v2.0 | Probes: 3 | BLE Ready`.

---

### Step 2: Android App Launch & BLE Discovery (01:30 – 03:00)
1. **Action:** Launch the GrainGuardian Android application on your smartphone.
2. **Examiner Talking Point:**
   > *"The mobile application is built natively in Kotlin using Jetpack Compose and an offline-first MVVM architecture. It supports English and regional Telugu localization for rural accessibility. The app initiates Bluetooth Low Energy scanning for Service UUID `12345678-1234-1234-1234-1234567890AB` and establishes a secure GATT connection without requiring an active internet connection."*
3. **Evidence:** Dashboard shows status change from `No Probe` to `Probe Online (BLE)` with live RSSI signal.

---

### Step 3: Live Safe Baseline Reading (03:00 – 04:30)
1. **Action:** Insert the probe into the baseline sample container (paddy at ~13.0% MC). Tap **"Check Stored Paddy"**.
2. **Examiner Talking Point:**
   > *"GrainGuardian collects 5 stable 1-second telemetry packets, executing 16-sample trimmed mean noise filtration. The GrainDecisionEngine calculates the Biological Degradation Risk Score (BDRS = 12/100) and Equilibrium Moisture Content (EMC). As moisture is 13.0% (below the 13.5% BIS threshold) and temperature gradient is 0.4°C, the system issues a normal SAFE verdict and stores the record in Room DB v2 with a cryptographically unique UUID."*

---

### Step 4: Inducing a Localized Thermal Hotspot (04:30 – 06:00)
1. **Action:** Hold the bottom sensor (T3) between fingers or immerse in warm water (raising temperature to ~35°C while T1 remains at 28°C).
2. **Examiner Talking Point:**
   > *"Grain spoilage in godowns is typically localized deep inside the pile due to insect respiration and fungal mycelium activity. Notice what happens when T3 reaches 35°C while surface T1 is 28°C:
   > 1. The temperature gradient ($\Delta T = 7.0^\circ\text{C}$) exceeds the critical $3.0^\circ\text{C}$ threshold.
   > 2. The ESP32 edge controller immediately trips the Red LED on GPIO 23 and activates the 2N2222 transistor buzzer on GPIO 25 without waiting for the phone.
   > 3. The Android app displays a high-contrast Red Alert banner and Android WorkManager triggers a heads-up background notification."*
3. **Evidence:** Buzzer sounds; OLED shows `HOTSPOT ALERT!`; Android shows `CRITICAL RISK (BDRS 75)`.

---

### Step 5: Pre-Storage Sun-Drying Advisor & PDF Certificate Export (06:00 – 07:30)
1. **Action:** Switch mode to **"Check Before Storage"** with a damp grain sample (16.5% MC). Tap **"Export Quality Certificate (PDF)"**.
2. **Examiner Talking Point:**
   > *"In pre-storage mode, drying recommendations are provided: the app calculates excess moisture to evaporate (+3.5%), recommends spreading grain at 4–5 cm thickness on drying yards, and specifies turning intervals of 1.5–2 hours. The farmer can tap 'Export Quality Certificate' to instantly generate a formal, vector A4 PDF inspection certificate containing complete thermometry tables, ICAR/BIS compliance notes, and a digital verification hash for mandi sale."*
3. **Evidence:** Generated PDF certificate opened on device.

---

### Step 6: Cloud Synchronization to FastAPI Backend (07:30 – 08:30)
1. **Action:** Turn on mobile data / Wi-Fi. In the app, open **Sync Status** and tap **"Sync Now"**.
2. **Examiner Talking Point:**
   > *"When network connectivity is restored, the durable CloudSyncManager transmits local un-synced inspections to our Python FastAPI cloud backend at `/api/v1/telemetry/sync`. The backend enforces idempotent batch inserts using UUID deduplication so duplicate packets are safely ignored. The central PostgreSQL warehouse records the inspection and automatically registers a critical risk event for godown managers."*
3. **Evidence:** App shows all records updated from `LOCAL` to `CONFIRMED`.

---

### Step 7: Examiner Q&A Defense Sheet (08:30 – 10:00)

| Potential Examiner Question | Strong Technical Answer |
| :--- | :--- |
| **Q1: Why use a capacitive moisture sensor instead of a resistive prong?** | *"Resistive sensors suffer from electrochemical corrosion, grain compaction sensitivity, and kernel surface polarization. The SEN0193 capacitive sensor uses high-frequency dielectric permittivity measurement ($\varepsilon_r \approx 80$ for water vs $3–5$ for starch) without bare copper contact, preventing rust and ensuring long-term repeatability."* |
| **Q2: What is BDRS and how is it calculated?** | *"Biological Degradation Risk Score (BDRS, 0–100) is a deterministic synthesis combining kernel moisture content, 3-depth temperature gradient index (TGI), and ambient equilibrium moisture content (EMC). A score below 25 denotes safe storage; 25–49 indicates attention required; $\ge 50$ signals active degradation requiring aeration or turning."* |
| **Q3: How does the system handle probe disconnection during a reading?** | *"The Dallas 1-Wire protocol returns error codes $-127.0^\circ\text{C}$ on open-circuit and $+85.0^\circ\text{C}$ on power-on reset. GrainGuardian strictly rejects these values via `isTemperaturePhysicallyValid()`. Under our agronomic safety laws, the system never falls back to a fake safe default (such as 13.2%); it flags an explicit `SENSOR_FAULT`."* |
| **Q4: How is duplicate data prevented during intermittent village connectivity?** | *"Every inspection generated in Room DB v2 is assigned an immutable RFC 4122 UUID v4. The FastAPI backend enforces a unique database index on `uuid`. Re-transmitting the batch triggers an SQL `ON CONFLICT DO NOTHING` / idempotent upsert, confirming the sync without duplicating warehouse statistics."* |

---

## 2. Hardware Pinout Diagram & Presentation Poster

### 2.1 Complete Pinout Connection Table

```
========================================================================================
ESP32 DevKit v1 Pin   Component / Subsystem     Pin Function     Electrical Component
========================================================================================
3V3 (Pin 1)           All Sensors & OLED        VCC (3.3V DC)    Regulated supply
GND (Pin 2 / 14)      Common Ground             GND (0V DC)      Star Ground topology
GPIO 4                DS18B20 Probes (x3)       1-Wire DQ Data   4.7 kΩ Pull-up to 3.3V
GPIO 34 (ADC1_CH6)    SEN0193 Moisture Probe    Analog Output    Direct to ADC1 (Input only)
GPIO 21               SSD1306 0.96" OLED        I2C SDA          Built-in OLED pull-ups
GPIO 22               SSD1306 0.96" OLED        I2C SCL          Built-in OLED pull-ups
GPIO 18               Status LED: GREEN         Safe Storage     220 Ω resistor to Anode
GPIO 19               Status LED: AMBER         Attention/Warn   220 Ω resistor to Anode
GPIO 23               Status LED: RED           Critical Hotspot 220 Ω resistor to Anode
GPIO 25               Piezo Buzzer (Active)     Alarm Actuator   2N2222 Transistor driver
========================================================================================
```

---

### 2.2 Circuit Schematic Architecture (ASCII Layout for Presentation Poster)

```text
                  +3.3V DC Rail
                     |
       +-------------+--------------+-------------+
       |             |              |             |
     [4.7k]          |              |             |
       |             |              |             |
       +--- GPIO 4   |              |             |
       |   (1-Wire)  |              |             |
    +--+--+       +--+--+        +--+--+       +--+--+
    |     |       |     |        |     |       |     |
    |T1Top|       |T2Mid|        |T3Bot|       | SEN |
    |     |       |     |        |     |       |0193 |
    +--+--+       +--+--+        +--+--+       +--+--+
       |             |              |             |
      GND           GND            GND            +--- GPIO 34 (ADC1)
                                                  |
                                                 GND

      ESP32 Microcontroller
   +-------------------------+
   |  GPIO 21 (SDA) ---------+----> SSD1306 OLED (SDA)
   |  GPIO 22 (SCL) ---------+----> SSD1306 OLED (SCL)
   |                         |
   |  GPIO 18 ----[ 220R ]---+----> [GREEN LED] ---> GND
   |  GPIO 19 ----[ 220R ]---+----> [AMBER LED] ---> GND
   |  GPIO 23 ----[ 220R ]---+----> [RED LED]   ---> GND
   |                         |
   |  GPIO 25 ----[  1k  ]---+----> Base of 2N2222 NPN Transistor
   +-------------------------+             |
                                  Collector|---> [ + Buzzer - ] ---> +5V / +3.3V
                                           |
                                    Emitter+---> GND
```

---

### 2.3 Bill of Materials (BOM) & Budget

| Item | Specification | Qty | Approx Cost (INR) |
| :--- | :--- | :---: | :---: |
| **ESP32 DevKit v1** | Dual-core Tensilica Xtensa, Wi-Fi & BLE | 1 | ₹450 |
| **DS18B20 Probes** | Waterproof stainless steel probe with 1m cable | 3 | ₹450 |
| **SEN0193 Moisture** | Corrosion-resistant capacitive analog probe | 1 | ₹280 |
| **SSD1306 OLED** | 0.96" I2C monochrome display (128x64) | 1 | ₹220 |
| **Status LEDs & Resistors**| 5mm Green/Amber/Red LEDs + 220Ω & 4.7kΩ resistors | 1 set | ₹50 |
| **Buzzer Circuit** | Active 5V piezo buzzer + 2N2222 NPN transistor | 1 | ₹40 |
| **Probe Pipe Housing** | 1-inch CPVC / acrylic hollow lance pipe (1m) | 1 | ₹180 |
| **Power Source** | 5V / 2A USB power bank or 18650 Li-ion shield | 1 | ₹350 |
| **TOTAL PROJECT COST**| | | **₹2,020 (~$25 USD)** |
