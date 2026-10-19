# GrainGuardian Cloud Telemetry & Analytics Backend

The **GrainGuardian Backend** is a high-performance, asynchronous REST API service built with **FastAPI**, **SQLAlchemy ORM**, **Pydantic v2**, and **Alembic migrations**. It powers central warehouse monitoring, multi-probe telemetry ingestion, thermal hotspot alerting, farmer authentication, and multi-farm device tracking.

---

## 1. Architectural Highlights

- **Offline-First Idempotent Synchronization:** Uses client-generated UUIDs (`uuid` / `sync_uuid`) to prevent duplicate database records during intermittent 4G/5G agricultural network reconnections.
- **Automated Thermal Hotspot Engine:** Automatically analyzes vertical thermal stratification ($T_1, T_2, T_3$), computes the Temperature Gradient Index ($\text{TGI} = T_{\text{max}} - T_{\text{min}}$), and triggers real-time bilingual risk events (English & Telugu) if $\text{TGI} \ge 3.0^\circ\text{C}$ or $T_{\text{max}} \ge 35.0^\circ\text{C}$.
- **Warehouse Summary Analytics:** Aggregates real-time moisture averages, peak probe temperatures, and risk breakdowns for PACS godown operators.
- **Relational Schema Migrations:** Managed through Alembic (`alembic upgrade head`).

---

## 2. API Endpoints Directory

All endpoints are versioned under `/api/v1/`:

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :---: |
| `GET` | `/health` | Service liveness and database probe | No |
| `POST` | `/auth/register` | Create a new farmer account | No |
| `POST` | `/auth/login` | OAuth2 password bearer token generation | No |
| `GET` | `/auth/me` | Fetch active farmer profile | **Yes (Bearer)** |
| `POST` | `/devices/register` | Register or update probe hardware MAC & farm location | No |
| `GET` | `/devices` | List all registered grain probes | No |
| `POST` | `/farms` | Create a new farm or warehouse location | No |
| `GET` | `/farms` | List registered farm locations | No |
| `POST` | `/telemetry/sync` | **Idempotent batch sync** for Room DB & LittleFS records | No |
| `GET` | `/telemetry/latest` | Get the most recent reading for a given probe MAC | No |
| `GET` | `/telemetry/history` | Paginated historical readings with crop & status filters | No |
| `GET` | `/telemetry/summary` | Warehouse-wide moisture averages and thermal extremes | No |
| `GET` | `/storage/risks` | List unacknowledged or filtered risk incidents | No |
| `POST` | `/storage/risks/{id}/acknowledge` | Acknowledge a risk incident | No |

---

## 3. Quickstart & Local Execution

### Prerequisites
- Python 3.10+ (Tested on Python 3.12)
- Virtual Environment: `.venv`

### Step 1: Activate Virtual Environment
```bash
# Windows PowerShell
.venv\Scripts\Activate.ps1

# Linux / macOS
source .venv/bin/activate
```

### Step 2: Run Database Migrations
```bash
alembic upgrade head
```

### Step 3: Launch Development Server
```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Interactive OpenAPI Swagger UI is available at:
`http://localhost:8000/docs`

---

## 4. Connecting from Android Emulator & Physical Devices

- **Android Emulator:** The host loopback is mapped to `10.0.2.2`. Configure server endpoint in the app's **Cloud Synchronization Screen** as:
  `http://10.0.2.2:8000/api/v1/telemetry/sync`
- **Physical Phone on Local Wi-Fi:** Bind uvicorn to `0.0.0.0` and use your host machine's LAN IP (e.g., `http://192.168.1.15:8000/api/v1/telemetry/sync`).

---

## 5. Running Automated Integration Tests

Execute the full pytest suite:
```bash
pytest
```
Expected output:
```
tests/test_auth.py ...
tests/test_devices.py ..
tests/test_farms.py .
tests/test_health.py ..
tests/test_storage.py .
tests/test_telemetry.py .....
============================== 14 passed in 1.8s ==============================
```
