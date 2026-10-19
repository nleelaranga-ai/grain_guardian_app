def test_register_and_login_success(client):
    # Register new user
    reg_response = client.post(
        "/api/v1/auth/register",
        json={
            "username": "test_farmer",
            "password": "farmer_password_123",
            "full_name": "Test Farmer",
            "email": "test@farmer.org"
        }
    )
    assert reg_response.status_code == 201
    user_data = reg_response.json()
    assert user_data["username"] == "test_farmer"
    assert user_data["is_active"] is True

    # Login with valid credentials
    login_response = client.post(
        "/api/v1/auth/token",
        data={
            "username": "test_farmer",
            "password": "farmer_password_123"
        }
    )
    assert login_response.status_code == 200
    token_data = login_response.json()
    assert "access_token" in token_data
    assert token_data["token_type"] == "bearer"
    assert token_data["expires_in"] > 0

def test_login_invalid_password_fails(client):
    # Register user
    client.post(
        "/api/v1/auth/register",
        json={
            "username": "farmer_login_test",
            "password": "correct_password"
        }
    )

    # Attempt login with wrong password
    response = client.post(
        "/api/v1/auth/token",
        data={
            "username": "farmer_login_test",
            "password": "wrong_password"
        }
    )
    assert response.status_code == 401
    assert "Incorrect username or password" in response.json()["detail"]

def test_duplicate_username_registration_fails(client):
    # Register first user
    res1 = client.post(
        "/api/v1/auth/register",
        json={
            "username": "duplicate_user",
            "password": "password123"
        }
    )
    assert res1.status_code == 201

    # Attempt to register same username again
    res2 = client.post(
        "/api/v1/auth/register",
        json={
            "username": "duplicate_user",
            "password": "another_password"
        }
    )
    assert res2.status_code == 400
    assert "already registered" in res2.json()["detail"]
