def test_register_device_and_query(client):
    reg_response = client.post(
        "/api/v1/devices/register",
        json={
            "device_mac": "24:6F:28:AB:CD:EF",
            "device_name": "GrainGuardian-Alpha",
            "farm_location": "Bapatla Shed 2",
            "hardware_version": "v2.0"
        }
    )
    assert reg_response.status_code == 201
    data = reg_response.json()
    assert data["device_mac"] == "24:6F:28:AB:CD:EF"
    assert data["device_name"] == "GrainGuardian-Alpha"

    # Query all registered devices
    list_response = client.get("/api/v1/devices")
    assert list_response.status_code == 200
    devices = list_response.json()
    assert len(devices) >= 1
    assert any(d["device_mac"] == "24:6F:28:AB:CD:EF" for d in devices)

def test_device_re_registration_updates_location(client):
    update_response = client.post(
        "/api/v1/devices/register",
        json={
            "device_mac": "24:6F:28:AB:CD:EF",
            "device_name": "GrainGuardian-Alpha-Relocated",
            "farm_location": "Tenali Warehouse 1",
            "hardware_version": "v2.0"
        }
    )
    assert update_response.status_code == 201
    data = update_response.json()
    assert data["device_name"] == "GrainGuardian-Alpha-Relocated"
    assert data["farm_location"] == "Tenali Warehouse 1"
