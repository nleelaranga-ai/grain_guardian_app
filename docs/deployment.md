# GrainGuardian — Deployment & Build Runbook v2.0

**Target Systems:** Android Hardware Devices (API 26+), ESP32-WROOM-32 Microcontrollers, Optional Dockerized Backend

---

## 1. Android Mobile Application Deployment

### Prerequisites
- JDK 17 (Recommended: Android Studio JBR)
- Android SDK 33 (Build Tools 33.0.1+)
- Android device running Android 8.0 (Oreo) or higher with Bluetooth Low Energy support.

### Building & Installing the APK
```powershell
# 1. Clone repository and navigate to root
cd C:\Users\LEELA RANGA PRASAD\.gemini\antigravity\scratch\GrainGuardian

# 2. Compile Debug APK
.\gradlew assembleDebug

# 3. Locate compiled artifact
# Path: app/build/outputs/apk/debug/app-debug.apk (15.6 MB)

# 4. Install onto connected Android device via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 2. ESP32 Firmware Flashing

### Prerequisites
- Arduino IDE 2.x or PlatformIO Core
- Espressif ESP32 Board Support Package (v2.0.14 or v3.x)
- Libraries: `OneWire`, `DallasTemperature`, `DHT sensor library`, `LittleFS`

### Flashing via Arduino IDE
1. Open [`firmware/GrainGuardian_ESP32/GrainGuardian_ESP32.ino`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/firmware/GrainGuardian_ESP32/GrainGuardian_ESP32.ino).
2. Select Board: **ESP32 Dev Module** (or **DOIT ESP32 DEVKIT V1**).
3. Configuration:
   - CPU Frequency: `240MHz`
   - Flash Frequency: `80MHz`
   - Flash Size: `4MB (32Mb)`
   - Partition Scheme: **Default 4MB with spiffs/littlefs (1.2MB App / 1.5MB SPIFFS/LittleFS)**
   - Upload Speed: `921600`
4. Connect ESP32 via USB-C cable and select the corresponding COM port.
5. Click **Upload**.
6. Open Serial Monitor at **115200 baud** to verify sensor initialization.

---

## 3. Optional Backend Service Deployment (FastAPI & PostgreSQL)

For cooperative warehouses or centralized storage aggregators wishing to deploy the shared backend:

```powershell
# 1. Copy environment template
copy .env.example .env

# 2. Spin up PostgreSQL and FastAPI containers via Docker Compose
docker-compose up -d --build

# 3. Run database migrations
docker-compose exec backend alembic upgrade head

# 4. Verify API health
curl http://localhost:8000/api/v1/health
```
