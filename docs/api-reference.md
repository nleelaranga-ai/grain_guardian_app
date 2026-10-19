# GrainGuardian — Backend API Specification v2.0 (Target Interface)

**Architecture:** RESTful OpenAPI 3.0  
**Backend Framework:** Python FastAPI / Pydantic v2 / SQLAlchemy / PostgreSQL  
**Authentication:** JWT Bearer Token (Header: `Authorization: Bearer <token>`)  

---

## 1. System Health & Authentication Endpoints

### `GET /api/v1/health`
Returns system operational status, database connectivity, and version.

**Response `200 OK`:**
```json
{
  "status": "healthy",
  "version": "2.0.0",
  "timestamp": "2026-10-19T00:30:00Z",
  "database": "connected"
}
```

### `POST /api/v1/auth/token`
Exchanges user credentials for JWT access token.

**Request Body (`application/x-www-form-urlencoded`):**
```
username=farmer_ramu&password=secure_password
```

**Response `200 OK`:**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer",
  "expires_in": 86400
}
```

---

## 2. Device Registration & Telemetry Ingestion

### `POST /api/v1/devices/register`
Registers an ESP32 hardware probe to a farmer account.

**Request Body:**
```json
{
  "device_mac": "24:6F:28:AB:CD:EF",
  "device_name": "GrainGuardian-01",
  "farm_location": "Bapatla, AP",
  "hardware_version": "v2.0"
}
```

### `POST /api/v1/telemetry/sync`
Idempotent batch upload of offline historical records retrieved via BLE from LittleFS.

**Request Body:**
```json
{
  "device_mac": "24:6F:28:AB:CD:EF",
  "records": [
    {
      "seq_id": 104,
      "timestamp": 1792348200000,
      "crop_name": "Paddy",
      "mode": "Storage Check",
      "moisture": 13.4,
      "t1_top": 25.38,
      "t2_middle": 25.56,
      "t3_bottom": 25.94,
      "ambient_humidity": 64.4,
      "status": "SAFE",
      "risk_score": 12,
      "dominant_risk": "Grain storage condition is optimal"
    }
  ]
}
```

**Response `201 Created`:**
```json
{
  "inserted": 1,
  "duplicates_skipped": 0,
  "status": "sync_complete"
}
```

---

## 3. Storage Risk Events & Advisory Endpoints

### `GET /api/v1/storage/risks`
Fetches active hotspots and moisture warning events across monitored farm lots.

**Query Parameters:**
- `status`: `"WARNING"` | `"HIGH_RISK"`
- `limit`: `50`
- `offset`: `0`

**Response `200 OK`:**
```json
{
  "total": 1,
  "events": [
    {
      "event_id": 482,
      "device_mac": "24:6F:28:AB:CD:EF",
      "crop_name": "Paddy",
      "tgi": 3.8,
      "peak_temp": 39.2,
      "status": "HIGH_RISK",
      "headline_en": "HIGH RISK - URGENT ACTION REQUIRED",
      "headline_te": "అత్యధిక ప్రమాదం - వెంటనే సరిచేయండి",
      "created_at": "2026-10-19T00:15:00Z",
      "acknowledged": false
    }
  ]
}
```
