def test_create_and_list_farms(client):
    # 1. Create a new farm location
    farm_payload = {
        "name": "Kaveri Delta Storage Warehouse",
        "location": "Thanjavur, Tamil Nadu"
    }
    create_res = client.post("/api/v1/farms", json=farm_payload)
    assert create_res.status_code == 201
    created_farm = create_res.json()
    assert created_farm["id"] is not None
    assert created_farm["name"] == farm_payload["name"]
    assert created_farm["location"] == farm_payload["location"]

    # 2. List all farms
    list_res = client.get("/api/v1/farms")
    assert list_res.status_code == 200
    farms = list_res.json()
    assert len(farms) >= 1
    matching = next((f for f in farms if f["name"] == farm_payload["name"]), None)
    assert matching is not None
    assert matching["id"] == created_farm["id"]
