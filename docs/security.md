# GrainGuardian — Security & Data Protection Specification v2.0

**Security Posture:** Offline Edge Zero-Trust  
**Target Environments:** Android Runtime, BLE Radio Subsystem, Local SQLite Storage  

---

## 1. Threat Modeling & Edge Risk Assessment

| Threat Vector | Severity | Mitigation Implemented |
| :--- | :---: | :--- |
| **False-Safe Spoiling Verdict** | **CRITICAL** | Removed dangerous 13.2% fallback. Missing, zero, or corrupt sensor data triggers explicit `MeasurementState.Error` and blocks safety certification. |
| **Unauthorized BLE Spoofing** | **MEDIUM** | ESP32 uses dedicated 128-bit Service and Characteristic UUIDs. Client validates incoming packet schemas (`Key=Value`) and ignores malformed inputs. |
| **Local Data Tampering** | **LOW** | SQLite database is encapsulated within Android's sandboxed private app directory (`/data/data/com.vrsec.grainguardian/databases/`). Other apps cannot access records without root access. |
| **Credential & Secret Exposure** | **HIGH** | The mobile client contains **zero hardcoded API keys, passwords, private tokens, or cloud credentials**. No sensitive data is transmitted over internet. |
| **Silicon ADC Contamination** | **HIGH** | Dedicated assignment of capacitive sensor to ADC1 (`GPIO 34`). ADC2 is never used, eliminating Bluetooth RF contamination of analog readings. |

---

## 2. Secrets & Credential Policy

1. **Zero Cloud Tokens in Mobile App:** Because GrainGuardian is an offline-first edge architecture, no master API keys or database connection strings are bundled into the APK.
2. **Environment Variables for Backend:** When deploying optional backend services (FastAPI/PostgreSQL), all database URLs, JWT signing secrets, and admin passwords must be supplied via `.env` files and never committed to version control.
3. **Repository Secret Hygiene:** `.gitignore` excludes all keystores (`*.jks`, `*.keystore`), `local.properties`, build caches, and private environment files.
