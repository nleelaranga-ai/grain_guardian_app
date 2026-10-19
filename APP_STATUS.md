# 🌾 GrainGuardian 2.0 — Master Technical Documentation & Complete System Status
**Full-Stack Smart Paddy Post-Harvest Readiness & Storage Health Intelligence System**

---

## 📌 Executive Summary

| Attribute | Details |
| :--- | :--- |
| **Product Name** | **GrainGuardian 2.0** (పంట రక్షకుడు) |
| **System Classification** | Full-Stack Offline-First IoT Storage Intelligence & Cloud Analytics Platform |
| **Target Agronomic Focus** | Paddy (*Oryza sativa*), Maize, Wheat, Pulses |
| **Target Users** | Smallholder Farmers, Custom Hiring Centers (CHCs), Paddy Millers, PACS, Central/State Warehouses |
| **System Architecture** | **3-Tier Distributed Architecture:**<br>1. **Edge Node:** ESP32 Hardware + LittleFS Autonomous Logging + BLE 4.2/5.0<br>2. **Mobile Client:** Android Native (Jetpack Compose + MVVM/UDF + Room SQLite + OkHttp)<br>3. **Enterprise Cloud & Web:** FastAPI + PostgreSQL 16 + Alembic + Next.js 14 App Router |
| **Application ID** | `com.vrsec.grainguardian` (Android Target SDK 33, Min SDK 26, JVM 11) |
| **Automated Test Scorecard** | **28 / 28 Automated Verifications Passing (100% Green)**<br>• Android Unit Tests: 18 / 18 Passing<br>• FastAPI Backend Pytest: 9 / 9 Passing (0 warnings)<br>• Next.js 14 Web Build: Compiled & Prerendered Successfully |
| **Core Safety Invariant** | **Zero False-Safe Defaults** — Sensor errors ($\le 0.0\%$ moisture) never default to $13.2\%$. Disconnected probes ($-127.0^\circ\text{C}$) are flagged as `ERR`. |

---

## ⚡ Terminal Navigation & Command Reference Guide

### ⚠️ Common Navigation Pitfalls & Solutions

When executing commands in PowerShell or Command Prompt, ensure your terminal is in the correct working directory for each respective tier:

```
C:\Users\LEELA RANGA PRASAD\.gemini\antigravity\scratch\GrainGuardian\  (ROOT)
├── app/          <-- Android Client
├── backend/      <-- FastAPI Backend & Python Services
├── firmware/     <-- ESP32 Arduino Sketch & Wiring
└── web/          <-- Next.js 14 Web Dashboard
```

| Mistake / Error | Cause | Proper Resolution |
| :--- | :--- | :--- |
| `cd web : Cannot find path ...\backend\web` | Executed while already inside `backend/` | Run `cd ..\web` to go up one directory, or `cd C:\Users\LEELA RANGA PRASAD\.gemini\antigravity\scratch\GrainGuardian\web` |
| `npm error enoent Could not read package.json` | Executed `npm run build` inside `backend/` | Switch to the web directory first (`cd ..\web`), then execute `npm.cmd run build` |
| `docker-compose : The term 'docker-compose' is not recognized` | Legacy hyphenated syntax or Docker Desktop not installed | 1. If using Docker Desktop: run `docker compose up -d --build` (with a space) from the **project root**.<br>2. If Docker is not installed: run the services **natively** using Python and Node (see below). |

---

### Step-by-Step Native Execution (Without Docker)

You do **not** need Docker to run and test GrainGuardian 2.0. All components run natively on Windows:

#### 1. Cloud Backend Service (FastAPI + Uvicorn)
```powershell
# Navigate to backend directory
cd "C:\Users\LEELA RANGA PRASAD\.gemini\antigravity\scratch\GrainGuardian\backend"

# Run automated tests (9 tests, 100% passing)
.\.venv\Scripts\pytest -v

# Start the live FastAPI server (runs on http://127.0.0.1:8000)
.\.venv\Scripts\uvicorn app.main:app --reload --port 8000
```
- Interactive Swagger API Docs: [http://127.0.0.1:8000/docs](http://127.0.0.1:8000/docs)
- Alternative Redoc Docs: [http://127.0.0.1:8000/redoc](http://127.0.0.1:8000/redoc)
- Health Check: [http://127.0.0.1:8000/api/v1/health](http://127.0.0.1:8000/api/v1/health)

#### 2. Enterprise Web Dashboard (Next.js 14 + Tailwind CSS)
```powershell
# Navigate to web directory (from backend, use cd ..\web; from root, use cd web)
cd "C:\Users\LEELA RANGA PRASAD\.gemini\antigravity\scratch\GrainGuardian\web"

# Start development server (runs on http://localhost:3000)
npm.cmd run dev

# Or compile production optimized build
npm.cmd run build
```

#### 3. Android Mobile Application
```powershell
# Navigate to project root
cd "C:\Users\LEELA RANGA PRASAD\.gemini\antigravity\scratch\GrainGuardian"

# Run all 18 Android unit tests
.\gradlew test

# Build debug APK
.\gradlew assembleDebug
```
The compiled APK will be at: `app\build\outputs\apk\debug\app-debug.apk`

---

## 🏛️ System Architecture

```mermaid
graph TD
    subgraph ESP32 Hardware Node
        S1["Capacitive Moisture Sensor (GPIO 34 ADC1)"] -->|Analog mV| ESP["ESP32 Microcontroller"]
        S2["DS18B20 Temp Top 10cm (GPIO 4)"] -->|1-Wire Digital| ESP
        S3["DS18B20 Temp Mid 40cm (GPIO 4)"] -->|1-Wire Digital| ESP
        S4["DS18B20 Temp Bottom 80cm (GPIO 4)"] -->|1-Wire Digital| ESP
        S5["DHT11 Ambient Temp & Humidity (GPIO 27)"] -->|Digital GPIO| ESP
        ESP -->|Autonomous 5-min Log| FS[("LittleFS Binary History /grain_history.bin")]
        ESP -->|GATT Characteristic 0x1234...AC| BLE_RADIO(("BLE Radio"))
    end

    subgraph Android Client Layer (com.vrsec.grainguardian)
        BLE_RADIO -.->|Live Packets & HIST Sync| BLE_MGR["BleManager.kt"]
        BLE_MGR -->|Reconstitute Timestamp from Uptime| VM["GrainViewModel.kt"]
        VM -->|Evaluate Safety & TGI| ENGINE["GrainDecisionEngine.kt"]
        VM -->|Save Validated Records| REPO["InspectionRepository.kt"]
        REPO -->|Temporal Deduplication +-30s| ROOM[("Room SQLite DB (Offline-First)")]
        REPO -->|Non-Blocking Async Upload| CLOUD_SYNC["CloudSyncManager.kt (OkHttp)"]
        VM -->|StateFlow UI Stream| COMPOSE["Jetpack Compose UI (18 Screens)"]
        LOC["LocalizationManager.kt"] -->|Reactive English / Telugu| COMPOSE
    end

    subgraph Cloud Backend (FastAPI + PostgreSQL)
        CLOUD_SYNC -->|POST /api/v1/telemetry/sync| API_GATEWAY["FastAPI Application"]
        API_GATEWAY -->|Bearer JWT Validation| AUTH_SRV["Security & Auth Engine (bcrypt)"]
        API_GATEWAY -->|Telemetry Persistence| DB[("PostgreSQL 16 / SQLite Head")]
        API_GATEWAY -->|Hotspot & Stratification Check| RISK_ENGINE["Auto-Risk Detection Engine"]
        RISK_ENGINE -->|Flag Hotspots & TGI > 4.0C| RISK_ALERTS[("Risk Events Table")]
    end

    subgraph Enterprise Web Dashboard (Next.js 14)
        WEB_APP["Next.js 14 App Router"] -->|REST Queries| API_GATEWAY
        WEB_APP --> G_OVERVIEW["Godown Overview (Capacity, Tonnage, Moisture)"]
        WEB_APP --> STRAT_CHART["3-Layer Thermal Stratification Visualizer"]
        WEB_APP --> RISK_TABLE["Active Hotspot & Spoilage Alert Table"]
        WEB_APP --> FLEET_VIEW["Hardware Probe Fleet & Sync Diagnostics"]
    end
```

---

## 🧪 Comprehensive Automated Test Scorecard

### Test Execution Verification (28 Tests Total — 100% Passing)

```
Test Execution Summary:
├── Android Test Suite (18 Tests):
│   ├── com.vrsec.grainguardian.domain.engine.GrainDecisionEngineTest
│   │   ├── [PASS] testNormalStorageThermalProfileReturnsSafe
│   │   ├── [PASS] testStorageHotspotGradientTriggersWarningOrHighRisk
│   │   ├── [PASS] testPaddySafeMoistureReturnsSafeDryingStatus
│   │   ├── [PASS] testMathematicalBoundsOfBDRSandGSHI
│   │   ├── [PASS] testDifferentCropsAdhereToDistinctMoistureLimits
│   │   ├── [PASS] testPaddyHighRiskMoistureReturnsHighRiskDryingStatus
│   │   └── [PASS] testPaddyWarningMoistureReturnsWarningDryingStatus
│   ├── com.vrsec.grainguardian.data.bluetooth.SensorCalibrationTest
│   │   ├── [PASS] testLiveSerialReading2110mVYieldsSafeMoisture
│   │   ├── [PASS] testBaseline2100mVYieldsExact13Point5Percent
│   │   ├── [PASS] testDryAirVoltageClampsCorrectly
│   │   ├── [PASS] testZeroOrNegativeMillivoltsReturnsZero
│   │   └── [PASS] testWetGrainVoltageCalculatesHighMoisture
│   ├── com.vrsec.grainguardian.data.bluetooth.BleHistorySyncTest
│   │   ├── [PASS] testHistPacketParsing
│   │   ├── [PASS] testTimestampReconstitutionFromUptime
│   │   ├── [PASS] testDuplicatePacketFilter
│   │   └── [PASS] testCorruptedPacketHandling
│   └── com.vrsec.grainguardian.data.cloud.CloudSyncPayloadTest
│       ├── [PASS] testTelemetrySyncPayloadSerialization
│       └── [PASS] testNullSafetyAndNegativeValuesInPayload
│
├── FastAPI Backend Test Suite (9 Tests):
│   ├── tests/test_auth.py
│   │   ├── [PASS] test_register_and_login_success
│   │   ├── [PASS] test_login_invalid_password_fails
│   │   └── [PASS] test_duplicate_username_registration_fails
│   ├── tests/test_devices.py
│   │   ├── [PASS] test_register_device_and_query
│   │   └── [PASS] test_device_re_registration_updates_location
│   ├── tests/test_health.py
│   │   ├── [PASS] test_health_check
│   │   └── [PASS] test_root_endpoint
│   └── tests/test_telemetry.py
│       ├── [PASS] test_telemetry_sync_and_deduplication
│       └── [PASS] test_hotspot_telemetry_triggers_risk_event
│
└── Next.js 14 Enterprise Web Dashboard (1 Build Verification):
    └── [PASS] Production type-check & static prerendering (0 errors, 4 routes compiled)
```

---

## 📱 Android Client Portfolio (All 18 Screens Operational)

| # | Screen Name | File | Purpose & Agronomic Logic | Telugu Localization Context |
| :- | :--- | :--- | :--- | :--- |
| **1** | **Splash** | [`SplashScreen.kt`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/app/src/main/java/com/vrsec/grainguardian/ui/screens/SplashScreen.kt) | Sheaf logo, animation, initial state check | పంట రక్షకుడు |
| **2** | **Language Selection** | [`LanguageSelectionScreen.kt`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/app/src/main/java/com/vrsec/grainguardian/ui/screens/LanguageSelectionScreen.kt) | English vs తెలుగు switcher with instant recomposition | భాషను ఎంచుకోండి (ఇంగ్లీష్ / తెలుగు) |
| **3** | **Welcome Onboarding** | [`WelcomeScreen.kt`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/app/src/main/java/com/vrsec/grainguardian/ui/screens/WelcomeScreen.kt) | Aflatoxin prevention, moisture safety cards | రైతు పోర్టల్ ప్రారంభించండి |
| **4** | **Dashboard** | [`DashboardScreen.kt`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/app/src/main/java/com/vrsec/grainguardian/ui/screens/DashboardScreen.kt) | Operations central: BLE probe chip, GSHI score (0–100) | ప్రధాన డాష్‌బోర్డ్, నిల్వ ఆరోగ్య స్కోరు |
| **5** | **Crop Selection** | [`CropSelectionScreen.kt`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/app/src/main/java/com/vrsec/grainguardian/ui/screens/CropSelectionScreen.kt) | Paddy (13.5%), Maize (14.0%), Wheat (12.5%), Pulses (10.0%) | పంట రకం ఎంపిక (వరి, మొక్కజొన్న, గోధుమ) |
| **6** | **Assessment Mode** | [`AssessmentSelectionScreen.kt`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/app/src/main/java/com/vrsec/grainguardian/ui/screens/AssessmentSelectionScreen.kt) | Dual modes: Pre-storage Drying vs Bagged Storage Monitoring | ఆరబెట్టే సంసిద్ధత / నిల్వ ధాన్యం ఆరోగ్యం |
| **7** | **Connect Probe** | [`ConnectProbeScreen.kt`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/app/src/main/java/com/vrsec/grainguardian/ui/screens/ConnectProbeScreen.kt) | Real BLE scanner, offline/disabled Bluetooth alerts, lab sandbox | ప్రోబ్‌ను కనెక్ట్ చేయండి, బ్లూటూత్ స్కానింగ్ |
| **8** | **Prepare Measurement** | [`PrepareMeasurementScreen.kt`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/app/src/main/java/com/vrsec/grainguardian/ui/screens/PrepareMeasurementScreen.kt) | 3-step physical insertion protocol (vertical push, 15s settling) | కొలతకు సిద్ధం చేయండి |
| **9** | **Live Measurement** | [`LiveMeasurementScreen.kt`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/app/src/main/java/com/vrsec/grainguardian/ui/screens/LiveMeasurementScreen.kt) | Real-time packet telemetry, 3 temperatures, stability checklist | ప్రత్యక్ష కొలత, స్థిరత్వ తనిఖీ (ΔM, ΔT) |
| **10** | **Drying Result** | [`DryingResultScreen.kt`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/app/src/main/java/com/vrsec/grainguardian/ui/screens/DryingResultScreen.kt) | Safe to Store vs Further Drying verdict, solar drying hours | ఆరబెట్టే ఫలితం (నిల్వకు సురక్షితం / ఇంకా ఆరబెట్టాలి) |
| **11** | **Storage Result** | [`StorageResultScreen.kt`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/app/src/main/java/com/vrsec/grainguardian/ui/screens/StorageResultScreen.kt) | Stratification gradient ($\text{TGI} = \|T_3 - T_1\|$), risk category | నిల్వ ఫలితం, ఉష్ణోగ్రత ప్రవణత |
| **12** | **Risk Analysis** | [`RiskAnalysisScreen.kt`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/app/src/main/java/com/vrsec/grainguardian/ui/screens/RiskAnalysisScreen.kt) | Biological Degradation Risk Score (BDRS 0–100), root cause breakdown | ప్రమాద విశ్లేషణ, BDRS స్కోరు |
| **13** | **Recommendation** | [`RecommendationScreen.kt`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/app/src/main/java/com/vrsec/grainguardian/ui/screens/RecommendationScreen.kt) | Localized farmer advisory (sun drying, bag turning, aeration) | రైతు కార్యాచరణ సలహా |
| **14** | **Inspection History** | [`InspectionHistoryScreen.kt`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/app/src/main/java/com/vrsec/grainguardian/ui/screens/InspectionHistoryScreen.kt) | Searchable and filterable archive of past inspections | పరీక్ష రికార్డులు, గత చరిత్ర |
| **15** | **Inspection Details** | [`InspectionDetailsScreen.kt`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/app/src/main/java/com/vrsec/grainguardian/ui/screens/InspectionDetailsScreen.kt) | Deep historical audit showing all 5 sensor streams and BDRS score | వివరణాత్మక రికార్డు వివరాలు |
| **16** | **Help & Support** | [`HelpScreen.kt`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/app/src/main/java/com/vrsec/grainguardian/ui/screens/HelpScreen.kt) | Agronomic knowledge base (paddy moisture chart, maintenance) | సహాయ కేంద్రం, వ్యవసాయ మార్గదర్శకాలు |
| **17** | **Settings & Cloud Sync** | [`SettingsScreen.kt`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/app/src/main/java/com/vrsec/grainguardian/ui/screens/SettingsScreen.kt) | Language switcher, sandbox toggle, Cloud Sync configuration card | సెట్టింగ్‌లు, క్లౌడ్ సింక్రొనైజేషన్ |
| **18** | **Device Diagnostics** | [`DeviceInfoScreen.kt`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/app/src/main/java/com/vrsec/grainguardian/ui/screens/DeviceInfoScreen.kt) | Hardware telemetry, LittleFS flash sync triggers (`CMD:SYNC`) | పరికర సమాచారం, సెన్సార్ డయాగ్నస్టిక్స్ |

---

## 📡 Hardware & Firmware Specifications

```
ESP32 Pinout Mapping:
- GPIO 34 (ADC1_CH6): DFRobot SEN0193 Capacitive Moisture Sensor (Analog mV)
- GPIO  4 (1-Wire)  : 3x DS18B20 Digital Temperature Probes (4.7kΩ pull-up to 3.3V)
- GPIO 27 (Digital) : DHT11 Ambient Temperature & Humidity Sensor
- 3.3V & GND        : Sensor power rails
```

### Hardware Communication Protocol
- **Microcontroller:** ESP32 BLE (GATT Server)
- **Service UUID:** `12345678-1234-1234-1234-1234567890AB`
- **Characteristic UUID:** `12345678-1234-1234-1234-1234567890AC` (Read, Write, Notify)
- **Live Stream Packet:**
  ```
  M:<moisture>,T1:<top>,T2:<middle>,T3:<bottom>,H:<humidity>,B:<battery>
  ```
- **Flash History Synchronization Packets:**
  ```
  HIST:SEQ=<seq>,UP=<uptime>,M=<moisture>,T1=<top>,T2=<mid>,T3=<bot>,H=<hum>
  HIST_END:COUNT=<total>
  ```
- **Phone Command Control:**
  - `CMD:INFO`: Request firmware version, uptime, and stored record count.
  - `CMD:SYNC`: Trigger playback of LittleFS binary history packets.
  - `CMD:CLEAR_HISTORY`: Clear flash logs after verified sync.

---

## 🧮 Agronomic Decision Mathematics

1. **Moisture Deviation:**
   $$\Delta M = M_{\text{measured}} - M_{\text{safe}}$$
   *(For paddy: $M_{\text{safe}} = 13.5\%$)*
2. **Thermal Gradient Index (TGI):**
   $$\text{TGI} = |T_3 - T_1|$$
   *($\text{TGI} > 4.0^\circ\text{C}$ indicates convective moisture migration and microclimatic condensation)*
3. **Biological Degradation Risk Score (BDRS 0–100):**
   $$\text{BDRS} = 0.50 \cdot f(M) + 0.30 \cdot f(T_2) + 0.20 \cdot f(\text{TGI})$$
4. **Grain Storage Health Index (GSHI 0–100):**
   $$\text{GSHI} = 100 - \text{BDRS}$$
5. **Solar Drying Duration Estimate:**
   $$t_{\text{drying}} = \max(0, \text{round}(\Delta M \times 2.5))\text{ hours}$$

---

## 🚢 Deployment Architecture (Docker & CI/CD)

- **Root Environment File:** [`.env.example`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/.env.example)
- **Docker Compose:** [`docker-compose.yml`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/docker-compose.yml) provisions:
  - `backend`: FastAPI Python 3.12 service on port 8000
  - `db`: PostgreSQL 16 Alpine with persistent volume on port 5432
- **GitHub Actions CI:** [`.github/workflows/ci.yml`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/.github/workflows/ci.yml) executes on every push/PR:
  1. Android unit test suite (`./gradlew test`)
  2. Cloud backend Pytest suite (`pytest -v`)
  3. Next.js 14 Web production build (`npm run build`)
