import uuid

def test_telemetry_sync_and_deduplication(client):
    mac = "AA:BB:CC:DD:EE:01"
    base_time = 1792348200000

    # Sync first batch
    sync_payload = {
        "device_mac": mac,
        "records": [
            {
                "seq_id": 1,
                "timestamp": base_time,
                "crop_name": "Paddy",
                "mode": "Storage Check",
                "moisture": 13.2,
                "t1_top": 28.5,
                "t2_middle": 28.8,
                "t3_bottom": 29.0,
                "ambient_humidity": 64.0,
                "status": "SAFE",
                "risk_score": 10,
                "dominant_risk": "Optimal storage condition"
            },
            {
                "seq_id": 2,
                "timestamp": base_time + 300000,
                "crop_name": "Paddy",
                "mode": "Storage Check",
                "moisture": 13.3,
                "t1_top": 28.6,
                "t2_middle": 28.9,
                "t3_bottom": 29.1,
                "ambient_humidity": 64.2,
                "status": "SAFE",
                "risk_score": 11,
                "dominant_risk": "Optimal storage condition"
            }
        ]
    }

    res1 = client.post("/api/v1/telemetry/sync", json=sync_payload)
    assert res1.status_code == 201
    d1 = res1.json()
    assert d1["inserted"] == 2
    assert d1["duplicates_skipped"] == 0

    # Attempt to sync duplicate records (same timestamps within 30s)
    res2 = client.post("/api/v1/telemetry/sync", json=sync_payload)
    assert res2.status_code == 201
    d2 = res2.json()
    assert d2["inserted"] == 0
    assert d2["duplicates_skipped"] == 2

    # Query latest telemetry
    latest_res = client.get(f"/api/v1/telemetry/latest?device_mac={mac}")
    assert latest_res.status_code == 200
    latest_data = latest_res.json()
    assert latest_data["device_mac"] == mac
    assert latest_data["moisture"] == 13.3
    assert latest_data["status"] == "SAFE"


def test_uuid_idempotent_sync(client):
    mac = "AA:BB:CC:DD:EE:03"
    rec_uuid = str(uuid.uuid4())
    sync_payload = {
        "device_mac": mac,
        "records": [
            {
                "uuid": rec_uuid,
                "seq_id": 10,
                "timestamp": 1792350000000,
                "crop_name": "Paddy",
                "mode": "Storage Check",
                "moisture": 13.4,
                "t1_top": 29.0,
                "t2_middle": 29.5,
                "t3_bottom": 30.0,
                "ambient_humidity": 65.0,
                "status": "SAFE",
                "risk_score": 12,
                "dominant_risk": "Safe condition"
            }
        ]
    }

    # First attempt: successfully inserted
    res1 = client.post("/api/v1/telemetry/sync", json=sync_payload)
    assert res1.status_code == 201
    data1 = res1.json()
    assert data1["inserted"] == 1
    assert data1["duplicates_skipped"] == 0
    assert rec_uuid in data1["synced_uuids"]

    # Second attempt with identical UUID: skipped as duplicate, UUID still confirmed
    res2 = client.post("/api/v1/telemetry/sync", json=sync_payload)
    assert res2.status_code == 201
    data2 = res2.json()
    assert data2["inserted"] == 0
    assert data2["duplicates_skipped"] == 1
    assert rec_uuid in data2["synced_uuids"]


def test_hotspot_telemetry_triggers_risk_event(client):
    mac = "AA:BB:CC:DD:EE:02"
    hotspot_time = 1792349000000

    # Submit a reading with large thermal gradient (TGI = 40.5 - 34.0 = 6.5C >= 4.0C -> HIGH_RISK)
    hotspot_payload = {
        "device_mac": mac,
        "records": [
            {
                "seq_id": 99,
                "timestamp": hotspot_time,
                "crop_name": "Paddy",
                "mode": "Storage Check",
                "moisture": 16.8,
                "t1_top": 34.0,
                "t2_middle": 38.5,
                "t3_bottom": 40.5,
                "ambient_humidity": 75.0,
                "status": "HIGH_RISK",
                "risk_score": 85,
                "dominant_risk": "Severe bottom thermal hotspot and mould danger"
            }
        ]
    }

    sync_res = client.post("/api/v1/telemetry/sync", json=hotspot_payload)
    assert sync_res.status_code == 201
    assert sync_res.json()["inserted"] == 1

    # Query storage risks
    risk_res = client.get("/api/v1/storage/risks?status=HIGH_RISK")
    assert risk_res.status_code == 200
    risk_data = risk_res.json()
    assert risk_data["total"] >= 1
    matching_event = next((e for e in risk_data["events"] if e["device_mac"] == mac), None)
    assert matching_event is not None
    assert matching_event["status"] == "HIGH_RISK"
    assert matching_event["peak_temp"] == 40.5
    assert matching_event["tgi"] == 6.5
    assert "అత్యధిక ప్రమాదం" in matching_event["headline_te"]

    # Acknowledge the risk event
    ack_res = client.post(f"/api/v1/storage/risks/{matching_event['event_id']}/acknowledge")
    assert ack_res.status_code == 200
    assert ack_res.json()["status"] == "acknowledged"


def test_telemetry_history_and_summary(client):
    mac = "AA:BB:CC:DD:EE:04"
    sync_payload = {
        "device_mac": mac,
        "records": [
            {
                "seq_id": 1,
                "timestamp": 1792351000000,
                "crop_name": "Paddy",
                "mode": "Storage Check",
                "moisture": 13.0,
                "t1_top": 28.0,
                "t2_middle": 29.0,
                "t3_bottom": 30.0,
                "ambient_humidity": 62.0,
                "status": "SAFE",
                "risk_score": 8,
                "dominant_risk": "Safe"
            },
            {
                "seq_id": 2,
                "timestamp": 1792352000000,
                "crop_name": "Paddy",
                "mode": "Storage Check",
                "moisture": 14.8,
                "t1_top": 30.0,
                "t2_middle": 32.0,
                "t3_bottom": 34.0,
                "ambient_humidity": 70.0,
                "status": "WARNING",
                "risk_score": 55,
                "dominant_risk": "Elevated moisture"
            }
        ]
    }
    client.post("/api/v1/telemetry/sync", json=sync_payload)

    # Test history endpoint
    hist_res = client.get(f"/api/v1/telemetry/history?device_mac={mac}&size=10")
    assert hist_res.status_code == 200
    hist_data = hist_res.json()
    assert hist_data["total"] == 2
    assert len(hist_data["records"]) == 2
    assert hist_data["records"][0]["moisture"] in (13.0, 14.8)

    # Test summary endpoint
    summary_res = client.get(f"/api/v1/telemetry/summary?device_mac={mac}")
    assert summary_res.status_code == 200
    summary_data = summary_res.json()
    assert summary_data["total_inspections"] == 2
    assert summary_data["warning_count"] >= 1
    assert summary_data["safe_count"] >= 1
    assert summary_data["max_temperature"] >= 34.0
    assert 13.0 <= summary_data["avg_moisture"] <= 15.0


def test_telemetry_validation_rejection(client):
    mac = "AA:BB:CC:DD:EE:05"
    # Unrealistic moisture > 50%
    invalid_payload = {
        "device_mac": mac,
        "records": [
            {
                "seq_id": 1,
                "timestamp": 1792353000000,
                "crop_name": "Paddy",
                "mode": "Storage Check",
                "moisture": 99.9,
                "t1_top": 28.0,
                "t2_middle": 29.0,
                "t3_bottom": 30.0
            }
        ]
    }
    res = client.post("/api/v1/telemetry/sync", json=invalid_payload)
    assert res.status_code == 422
