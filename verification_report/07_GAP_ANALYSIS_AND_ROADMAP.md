# 07 — GAP ANALYSIS & ENGINEERING ROADMAP

**Scope:** Prioritized identification of missing features, safety vulnerabilities, hardware limitations, and future enhancements.

---

## 1. Prioritization Framework

- **P0 (Critical Baseline Integrity):** Required to prevent critical bugs, false-safe verdicts, or data loss in the core prototype.
- **P1 (Operational Reliability):** Required for autonomous, continuous, field-reliable farmer operation.
- **P2 (Advanced System Enhancements):** Non-blocking enhancements for commercial readiness or advanced analytics.

---

## 2. Prioritized Engineering Gaps

### P0 — Critical Baseline Integrity

#### Gap 1: Fallback Default Vulnerability in Measurement Flow
- **Why It Matters:** In [`GrainViewModel.kt#L217-L221`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/app/src/main/java/com/vrsec/grainguardian/viewmodel/GrainViewModel.kt#L217-L221), if `sensor.moisture <= 0f`, the code defaults to `13.2f`. Since $13.2\% \le 13.5\%$, a disconnected or broken moisture sensor falsely emits a **SAFE TO STORE** verdict!
- **Dependencies:** None.
- **Implementation Task:** Replace the fallback assignment with an explicit error gate:
  ```kotlin
  if (finalSensor.moisture <= 0f) {
      _measurementState.value = MeasurementState.Error(
          "Moisture sensor disconnected or reporting 0. Cannot compute verdict.",
          "తేమ సెన్సార్ పనిచేయడం లేదు. ఫలితం లెక్కించలేము."
      )
      return@launch
  }
  ```
- **Acceptance Test:** Disconnect the moisture probe pin `GPIO 34`. Proceed to measurement. Verify that the app halts and displays an error screen rather than completing with a "Safe to Store" verdict.

#### Gap 2: Missing Automated Unit Tests
- **Why It Matters:** Currently, `app/src/test` has 0 tests (`NO-SOURCE`). Any modification to `BleManager.kt` or `GrainDecisionEngine.kt` risks silent regressions without continuous integration safety.
- **Dependencies:** JUnit 4 / MockK.
- **Implementation Task:** Create `app/src/test/java/com/vrsec/grainguardian/GrainDecisionEngineTest.kt` verifying:
  - Paddy at $12.0\% \rightarrow$ SAFE
  - Paddy at $14.5\% \rightarrow$ WARNING
  - Paddy at $18.0\% \rightarrow$ HIGH_RISK
  - $\text{TGI} = 4.5^\circ\text{C} \rightarrow$ Hotspot detected
- **Acceptance Test:** Run `./gradlew test`. Verify at least 15 unit tests execute and pass with 100% green status.

---

### P1 — Operational Reliability

#### Gap 3: Autonomous Disconnected Logging (Mode B Continuous Monitoring)
- **Why It Matters:** Farmers do not leave their mobile phones in the grain godown 24 hours a day. When Bluetooth disconnects, the ESP32 must log readings to onboard flash memory so data is not lost.
- **Dependencies:** ESP32 `LittleFS.h` library, RTC timekeeping (DS3231 or internal timer).
- **Implementation Task:**
  - Allocate a 1 MB LittleFS partition on ESP32 SPI flash.
  - Append a 16-byte binary record every 5 minutes (`timestamp`, `moisture_mv`, `t1`, `t2`, `t3`, `hum`).
  - Implement a BLE synchronization command (`CMD_SYNC_HISTORY`) that flushes historical records to the Android app upon reconnection.
- **Acceptance Test:** Disconnect the phone for 2 hours (24 cycles). Reconnect the phone. Verify that 24 historical inspection rows appear in the Android Room database.

#### Gap 4: Physical ROM Address Binding for DS18B20 Probes
- **Why It Matters:** `ds18b20.getTempCByIndex(0..2)` sorts addresses lexicographically. If probe wires are swapped, $T_1$ (Top) might read bottom temperature, corrupting the thermal stratification model.
- **Dependencies:** OneWire ROM address scanner sketch.
- **Implementation Task:** Hardcode the 8-byte ROM address of each physical probe in firmware:
  ```cpp
  DeviceAddress probeTop    = { 0x28, 0xFF, 0x64, 0x1E, 0x82, 0x17, 0x04, 0x32 };
  DeviceAddress probeMiddle = { 0x28, 0xAA, 0x33, 0x12, 0x05, 0x16, 0x02, 0x88 };
  DeviceAddress probeBottom = { 0x28, 0xEE, 0x11, 0x44, 0x90, 0x18, 0x01, 0x41 };
  ```
- **Acceptance Test:** Warm the top probe with fingers. Verify that strictly $T_1$ rises in the serial monitor and mobile app.

#### Gap 5: Local OLED Display & Buzzer Activation in Firmware
- **Why It Matters:** When the farmer inspects bags without opening the smartphone app, the probe should provide immediate local feedback.
- **Dependencies:** `Adafruit_SSD1306.h`, `Adafruit_GFX.h`.
- **Implementation Task:** Wire I2C OLED to `GPIO 21` (SDA) and `GPIO 22` (SCL). Display real-time moisture %, $T_{\text{max}}$, and a high-pitch warning beep if $\text{TGI} > 3.0^\circ\text{C}$.
- **Acceptance Test:** Expose probe to high temperature ($>35^\circ\text{C}$). Verify OLED renders "HOTSPOT WARNING" and buzzer sounds.

---

### P2 — Advanced System Enhancements

#### Gap 6: Background Alerts via Android WorkManager
- **Why It Matters:** If the probe stays connected via BLE in a nearby room, the farmer should receive an Android push notification when a hotspot forms.
- **Dependencies:** Android `WorkManager` and `NotificationCompat`.
- **Implementation Task:** Schedule a periodic worker checking BLE status every 15 minutes. Emit high-priority notifications if $\text{BDRS} > 60$.
- **Acceptance Test:** Trigger high moisture reading. Put app in background. Verify native heads-up notification appears on Android lock screen.

#### Gap 7: PDF / CSV Inspection History Export
- **Why It Matters:** Farmers need inspection records to prove grain quality to millers, PACS procurement centers, or crop insurance surveyors.
- **Dependencies:** Android `PdfDocument` or CSV serializer.
- **Implementation Task:** Add an "Export to CSV/PDF" button in `InspectionHistoryScreen.kt` using Android Storage Access Framework.
- **Acceptance Test:** Tap "Export History". Verify a `.csv` file is created in Downloads containing all 14 database columns.
