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
