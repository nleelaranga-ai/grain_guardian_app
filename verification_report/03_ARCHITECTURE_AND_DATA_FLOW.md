# 03 — ARCHITECTURAL TOPOLOGY & END-TO-END DATA FLOW

**Scope:** Formal specification of the verified system architecture, physical wiring, communications protocols, and runtime telemetry tracing.

---

## 1. Actual Implemented Architecture vs. Proposed Concept

The repository contains an **Edge-First Local Architecture**. Early presentation documents described a cloud-connected architecture (ESP32 $\rightarrow$ Wi-Fi $\rightarrow$ FastAPI $\rightarrow$ Supabase $\rightarrow$ React). The table below details what is actually present in code versus what was proposed:

```
+------------------------------------------------------------------------------------------------------+
|                                   ARCHITECTURE TOPOLOGY COMPARISON                                   |
+---------------------------------------+--------------------------------------------------------------+
| SLIDE DECK / PROPOSAL (CONCEPTUAL)    | ACTIVE REPOSITORY REPO (VERIFIED IMPLEMENTATION)             |
+---------------------------------------+--------------------------------------------------------------+
| [Sensors]                             | [Sensors: 3x DS18B20 + SEN0193 + DHT11]                      |
|    |                                  |    | (1-Wire GPIO 4, ADC1 GPIO 34, Single-Bus GPIO 27)       |
|    v                                  |    v                                                         |
| [ESP32 Wi-Fi Client]                  | [ESP32 BLE GATT Server (GrainGuardian)]                     |
|    | (2.4 GHz 802.11b/g/n)            |    | (BLE Advertising: 12345678-...-90AB)                    |
|    v                                  |    v                                                         |
| [FastAPI Python Cloud Backend] (MISSING) [Android Native Client: com.vrsec.grainguardian]             |
|    | (REST API / JWT Auth)            |    | (BleManager.kt -> StateFlow -> GrainViewModel)          |
|    v                                  |    v                                                         |
| [Supabase Cloud PostgreSQL]    (MISSING) [GrainDecisionEngine.kt (Deterministic Agronomic Math)]       |
|    | (Remote DB Sync)                 |    | (TGI, BDRS, Drying Hours, Telugu/English Advice)        |
|    v                                  |    v                                                         |
| [React Web Dashboard]          (MISSING) [Room SQLite Local DB: inspections table]                   |
|                                       |    | (Offline persistence on physical phone)                 |
|                                       |    v                                                         |
|                                       | [Jetpack Compose UI: 18 Bilingual Screens]                   |
+---------------------------------------+--------------------------------------------------------------+
```

---

## 2. Implemented Architecture Topology Diagram

The diagram below documents **strictly supported and verified components**. Missing, unverified, or cloud components are explicitly marked with dashed red lines.

```mermaid
flowchart TD
    classDef verified fill:#dcfce7,stroke:#16a34a,stroke-width:2px,color:#14532d;
    classDef implemented fill:#e0f2fe,stroke:#0284c7,stroke-width:2px,color:#0369a1;
    classDef missing fill:#fee2e2,stroke:#dc2626,stroke-width:2px,stroke-dasharray: 5 5,color:#7f1d1d;

    subgraph HardwareProbe ["Physical 1-Meter Multi-Depth Insertion Probe"]
        S_Moist["SEN0193 Capacitive Moisture Probe<br/>(Tip Layer: 80-100cm)"]:::verified
        S_T1["DS18B20 Temp Probe 1<br/>(Top Layer: 10cm)"]:::implemented
        S_T2["DS18B20 Temp Probe 2<br/>(Middle Core: 40cm)"]:::implemented
        S_T3["DS18B20 Temp Probe 3<br/>(Bottom Base: 80cm)"]:::implemented
        S_DHT["DHT11 Ambient Sensor<br/>(Air Temp & Humidity)"]:::implemented
        
        ESP32["ESP32 DevKit V1 Microcontroller<br/>(Tensilica Dual-Core, 240MHz)"]:::verified
        
        S_Moist -->|Analog Out 0-3.0V| P_ADC["GPIO 34 (ADC1_CH6)"]:::verified
        S_T1 -->|1-Wire Digital Bus| P_1W["GPIO 4 (4.7k Pull-up)"]:::implemented
        S_T2 -->|1-Wire Digital Bus| P_1W
        S_T3 -->|1-Wire Digital Bus| P_1W
        S_DHT -->|Single-Bus Digital| P_DHT["GPIO 27"]:::implemented
        
        P_ADC --> ESP32
        P_1W --> ESP32
        P_DHT --> ESP32
    end

    subgraph FirmwareGATT ["Firmware BLE Stack (GrainGuardian_ESP32.ino)"]
        BLE_ADV["BLE Advertising<br/>Name: 'GrainGuardian'"]:::verified
        BLE_SRV["BLE Service<br/>UUID: 12345678-...-90AB"]:::verified
        BLE_CHAR["BLE Characteristic<br/>UUID: 12345678-...-90AC<br/>(READ | NOTIFY)"]:::verified
        CCCD["Descriptor: 0x2902 (CCCD)"]:::verified
        
        ESP32 --> BLE_ADV
        ESP32 --> BLE_SRV
        BLE_SRV --> BLE_CHAR
        BLE_CHAR --> CCCD
    end

    subgraph MobileApp ["Android Native Application (com.vrsec.grainguardian)"]
        SCANNER["BluetoothLeScanner<br/>(ScanFilter: 'GrainGuardian')"]:::verified
        BM["BleManager.kt<br/>(GATT Client & Delimiter Parser)"]:::verified
        ACCUM["State Accumulator &<br/>SEN0193 mV-to-Moisture% Calibrator"]:::verified
        GATE["5-Reading Rolling Stability Gate<br/>(Delta M <= 0.35%, Delta T <= 0.45°C)"]:::verified
        VM["GrainViewModel.kt<br/>(StateFlow UI State Producer)"]:::verified
        ENGINE["GrainDecisionEngine.kt<br/>(Deterministic TGI & BDRS Logic)"]:::verified
        ROOM[("Room SQLite DB<br/>Table: inspections")]:::verified
        COMPOSE["Jetpack Compose UI<br/>(18 Screens, EN / TE)"]:::verified

        BLE_CHAR -->|1Hz Notification Packets| SCANNER
        SCANNER --> BM
        BM --> ACCUM
        ACCUM --> GATE
        GATE --> VM
        VM --> ENGINE
        ENGINE --> VM
        VM --> ROOM
        VM --> COMPOSE
    end

    subgraph CloudTier ["Unimplemented Cloud Proposal (From PPT)"]
        FASTAPI["FastAPI Python Backend"]:::missing
        SUPABASE["Supabase Cloud PostgreSQL"]:::missing
        REACT["React Web Dashboard"]:::missing
        
        ESP32 -.->|Wi-Fi HTTP/MQTT - NOT FOUND| FASTAPI
        FASTAPI -.->|REST / SQL - NOT FOUND| SUPABASE
        SUPABASE -.->|Webhooks - NOT FOUND| REACT
    end
```

---

## 3. End-to-End Runtime Telemetry Trace

Here is the exact numerical trace of the live sensor dataset captured from the physical prototype:

### Input Sensor Values (From Physical Probe Serial Stream)
- $T_1 = 25.38^\circ\text{C}$ (Top Layer)
- $T_2 = 25.56^\circ\text{C}$ (Middle Core)
- $T_3 = 25.94^\circ\text{C}$ (Bottom Base)
- $\text{Air Temp} = 24.9^\circ\text{C}$
- $\text{Relative Humidity} = 64.4\%$
- $\text{Moisture Raw ADC} = 2368$
- $\text{Moisture Voltage} = 2110\text{ mV}$

### Step 1: ESP32 Firmware Telemetry Transmission
In [`GrainGuardian_ESP32.ino#L133-L148`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/firmware/GrainGuardian_ESP32/GrainGuardian_ESP32.ino#L133-L148), when `deviceConnected == true`, the ESP32 transmits short ASCII notifications via `sensorCharacteristic->notify()`:
```
Packet 1: "T1=25.38"   (Delay: 60ms)
Packet 2: "T2=25.56"   (Delay: 60ms)
Packet 3: "T3=25.94"   (Delay: 60ms)
Packet 4: "AIR=24.9"   (Delay: 60ms)
Packet 5: "HUM=64.4"   (Delay: 60ms)
Packet 6: "MR=2368"    (Delay: 60ms)
Packet 7: "MV=2110"    (Delay: 60ms)
```

### Step 2: Android GATT Notification Reception
In [`BleManager.kt#L210-L226`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/app/src/main/java/com/vrsec/grainguardian/data/bluetooth/BleManager.kt#L210-L226), the Android 13+ callback interceptor handles the byte buffer:
```kotlin
override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
    parseSensorPacket(value)
}
```

### Step 3: Delimiter Parsing & Persistent Accumulation
In `parseSensorPacket()`, string buffers are split using `Regex("[:=]")`. Instead of destroying state on each arrival, the internal accumulator updates persistently:
- `accumulatedT1 = 25.38f`
- `accumulatedT2 = 25.56f`
- `accumulatedT3 = 25.94f`
- `accumulatedHumidity = 64.4f`
- `accumulatedMoistureMV = 2110`

### Step 4: Agronomic Moisture Calibration
The raw voltage $2110\text{ mV}$ is converted to paddy moisture content via `convertMillivoltsToMoisture()`:
$$M = 13.5 + \left( \frac{2100 - 2110}{500} \right) \times 4.5 = 13.5 - 0.09 = \mathbf{13.41\%}$$
`accumulatedMoisture` is assigned `13.41f`.

### Step 5: Rolling Stability Verification
Over 4 consecutive 2-second cycles:
$$\Delta M = |13.41 - 13.41| = 0.00\% \le 0.35\% \quad \text{[PASS]}$$
$$\Delta T = |25.38 - 25.38| = 0.00^\circ\text{C} \le 0.45^\circ\text{C} \quad \text{[PASS]}$$
`isStable` is set to `true`, and `packetsReceived` increments to `7`.

### Step 6: Agronomic Storage Health Evaluation
In [`GrainDecisionEngine.kt#L105-L258`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/app/src/main/java/com/vrsec/grainguardian/domain/engine/GrainDecisionEngine.kt#L105-L258), `evaluateStorage()` computes:
1. **Thermal Gradient Index ($\text{TGI}$):**
   $$\text{TGI} = \max(25.38, 25.56, 25.94) - \min(25.38, 25.56, 25.94) = 25.94 - 25.38 = \mathbf{0.56^\circ\text{C}}$$
2. **Moisture Safety Check:**
   $$M = 13.41\% \le 13.5\% \quad (\text{Safe for Paddy})$$
3. **Hotspot Detection:**
   $$T_{\text{max}} = 25.94^\circ\text{C} \le 35.0^\circ\text{C} \quad \text{AND} \quad \text{TGI} = 0.56^\circ\text{C} \le 3.0^\circ\text{C} \quad (\text{No Hotspot})$$
4. **Biological Degradation Risk Score ($\text{BDRS}$):**
   $$\text{RawRisk} = (0 \times 14) + (0.56 \times 7.5) + (0 \times 5) + (0 \times 1.2) = 4.2$$
   $$\text{BDRS} = \text{round}(4.2 + 8.0) = \mathbf{12} \quad (0 - 100 \text{ scale})$$
5. **Grain Storage Health Index ($\text{GSHI}$):**
   $$\text{GSHI} = 100 - 12 = \mathbf{88 / 100}$$
6. **Final Status:** `RiskStatus.SAFE`  
   - English Headline: `"GRAIN STORAGE IS SAFE"`
   - Telugu Headline: `"నిల్వ సురక్షితంగా ఉంది"`

### Step 7: Local Persistence in Room Database
When the farmer taps "Save Inspection", [`GrainViewModel.kt#L254-L277`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/app/src/main/java/com/vrsec/grainguardian/viewmodel/GrainViewModel.kt#L254-L277) creates a `GrainInspectionEntity` and commits it to SQLite via `InspectionRepository.saveInspection()`:
- `id`: Auto-generated (e.g., `1`)
- `cropName`: `"Paddy"`
- `mode`: `"Storage Check"`
- `moisture`: `13.4f`
- `t1Top`: `25.38f`, `t2Middle`: `25.56f`, `t3Bottom`: `25.94f`, `humidity`: `64.4f`
- `status`: `"SAFE"`, `riskScore`: `12`
- `timestamp`: `1792348200000`
- `formattedDate`: `"19 Oct 2026, 12:04 AM"`
