# GrainGuardian 2.0 — Production Release APK

This directory contains the pre-compiled, signed production release APK for **GrainGuardian: Smart Paddy Post-Harvest Readiness and Storage Health Intelligence System**.

## Download APK
👉 **Direct File Link:** [`GrainGuardian-v2.0-release.apk`](GrainGuardian-v2.0-release.apk) (10.8 MB)

---

## Package Specifications
* **Package Name:** `com.vrsec.grainguardian`
* **Version:** `1.0.0` (VersionCode: `1`)
* **Minimum Android Version:** Android 7.0+ (`minSdkVersion = 24`)
* **Target Android Version:** Android 13 (`targetSdkVersion = 33`)
* **Signatures:** Verified with **v1 (JAR signing)**, **v2 (APK Signature Scheme)**, and **v3** schemes.
* **Build Type:** Release (Non-debuggable, optimized with D8 bytecode optimization and AAPT2 resource shrinkage).

---

## Installation Instructions

### Option 1: Direct Mobile Install (WhatsApp / Telegram / Browser)
1. Download `GrainGuardian-v2.0-release.apk` directly to your phone.
2. Tap the APK file to open the Android Package Installer.
3. If prompted with *"For security, your phone is not allowed to install unknown apps from this source"*, tap **Settings** and toggle **"Allow from this source"** to **ON**.
4. Tap **Install** $\rightarrow$ **Open**.

### Option 2: Via ADB (USB Cable or Wireless Debugging)
```bash
adb install -r release/GrainGuardian-v2.0-release.apk
```
