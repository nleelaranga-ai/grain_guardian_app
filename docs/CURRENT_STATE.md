# GrainGuardian — Current Codebase State Snapshot (Phase 0)

This document records the exact state of the project inspected directly from the codebase prior to executing Phase 1 modifications.

---

## 1. Toolchain & Framework Versions

- **Kotlin Version:** `1.8.20` (from root `build.gradle.kts`)
- **Android Gradle Plugin (AGP):** `7.4.2` (from root `build.gradle.kts`)
- **KSP Version:** `1.8.20-1.0.11` (from root `build.gradle.kts`)
- **Target SDK:** `33` (Android 13)
- **Min SDK:** `26` (Android 8.0 Oreo)
- **Compile SDK:** `33`
- **JVM Target:** `11`
- **Compose Compiler Extension:** `1.4.6` (from `app/build.gradle.kts`)
- **Compose BOM:** `androidx.compose:compose-bom:2023.04.01` (from `app/build.gradle.kts`)
- **Navigation Compose:** `2.6.0` (from `app/build.gradle.kts`)
- **Room Database:** `2.5.2` (from `app/build.gradle.kts`)

---

## 2. Room Database Configuration

- **Database Class:** `GrainGuardianDatabase.kt`
- **Current Room Schema Version:** `1` (`version = 1`, `exportSchema = false`)
- **Entities:** `GrainInspectionEntity::class` (`grain_inspections` table)
- **Current Entity Fields:**
  - `id` (Long, auto-generate PK)
  - `timestamp` (Long)
  - `cropType` (CropType enum)
  - `assessmentMode` (AssessmentMode enum)
  - `moisture` (Float)
  - `tempTop` (Float)
  - `tempMiddle` (Float)
  - `tempBottom` (Float)
  - `humidity` (Float)
  - `batteryLevel` (Int)
  - `riskStatus` (RiskStatus enum)
  - `riskScore` (Int)
  - `dryingHours` (Int)
  - `storageLifeDays` (Int)
  - `summaryEn` (String)
  - `summaryTe` (String)
  - `isSandbox` (Boolean)

---

## 3. Screens Present (18 Screens)

Registered in `com.vrsec.grainguardian.ui.navigation.Screen`:

1. `Splash` (`SplashScreen.kt`)
2. `LanguageSelection` (`LanguageSelectionScreen.kt`)
3. `Welcome` (`WelcomeScreen.kt`)
4. `Dashboard` (`DashboardScreen.kt`)
5. `CropSelection` (`CropSelectionScreen.kt`)
6. `AssessmentSelection` (`AssessmentSelectionScreen.kt`)
7. `ConnectProbe` (`ConnectProbeScreen.kt`)
8. `PrepareMeasurement` (`PrepareMeasurementScreen.kt`)
9. `LiveMeasurement` (`LiveMeasurementScreen.kt`)
10. `DryingResult` (`DryingResultScreen.kt`)
11. `StorageResult` (`StorageResultScreen.kt`)
12. `RiskAnalysis` (`RiskAnalysisScreen.kt`)
13. `Recommendation` (`RecommendationScreen.kt`)
14. `InspectionHistory` (`InspectionHistoryScreen.kt`)
15. `InspectionDetails` (`InspectionDetailsScreen.kt`)
16. `Help` (`HelpScreen.kt`)
17. `Settings` (`SettingsScreen.kt`)
18. `DeviceInfo` (`DeviceInfoScreen.kt`)

---

## 4. Bluetooth Low Energy (BLE) Protocol & UUIDs

Defined in `com.vrsec.grainguardian.data.bluetooth.BleManager`:

- **Primary GATT Service UUID:** `12345678-1234-1234-1234-1234567890AB` (Matches physical ESP32 firmware)
- **Primary GATT Characteristic UUID:** `12345678-1234-1234-1234-1234567890AC` (Notify / Write / Read)
- **Fallback GATT Service UUID:** `0000FFE0-0000-1000-8000-00805F9B34FB` (16-bit short form `0xFFE0`)
- **Fallback GATT Characteristic UUID:** `0000FFE1-0000-1000-8000-00805F9B34FB` (16-bit short form `0xFFE1`)
- **Client Characteristic Configuration Descriptor (CCCD):** `00002902-0000-1000-8000-00805F9B34FB`
