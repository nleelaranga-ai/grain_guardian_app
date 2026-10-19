def test_storage_risks_filtering_and_acknowledgement(client):
    mac = "AA:BB:CC:DD:EE:06"
    # Create telemetry that triggers a WARNING event (Moisture = 15.2%, TGI = 2.0C)
    warning_payload = {
        "device_mac": mac,
        "records": [
            {
                "seq_id": 1,
                "timestamp": 1792360000000,
                "crop_name": "Paddy",
                "mode": "Storage Check",
                "moisture": 15.2,
                "t1_top": 28.0,
                "t2_middle": 29.0,
                "t3_bottom": 30.0,
                "status": "WARNING",
                "risk_score": 45,
                "dominant_risk": "Elevated moisture warning"
            }
        ]
    }
    sync_res = client.post("/api/v1/telemetry/sync", json=warning_payload)
    assert sync_res.status_code == 201

    # Query risks filtered by WARNING
    res = client.get("/api/v1/storage/risks?status=WARNING")
    assert res.status_code == 200
    data = res.json()
    assert data["total"] >= 1
    event = next((e for e in data["events"] if e["device_mac"] == mac), None)
    assert event is not None
    assert event["status"] == "WARNING"
    assert event["acknowledged"] is False

    # Acknowledge the event
    ack_res = client.post(f"/api/v1/storage/risks/{event['event_id']}/acknowledge")
    assert ack_res.status_code == 200
    assert ack_res.json()["status"] == "acknowledged"

    # Acknowledge non-existent event gives 404
    ack_404 = client.post("/api/v1/storage/risks/999999/acknowledge")
    assert ack_404.status_code == 404
