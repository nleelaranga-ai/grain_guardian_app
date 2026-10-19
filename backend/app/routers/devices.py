from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from ..database import get_db
from ..models.device import Device
from ..schemas.device import DeviceRegisterRequest, DeviceResponse

router = APIRouter(prefix="/devices", tags=["Devices"])

@router.post("/register", response_model=DeviceResponse, status_code=status.HTTP_201_CREATED)
def register_device(payload: DeviceRegisterRequest, db: Session = Depends(get_db)):
    mac_clean = payload.device_mac.strip().upper()
    existing = db.query(Device).filter(Device.device_mac == mac_clean).first()
    if existing:
        # Update existing device info
        existing.device_name = payload.device_name
        existing.farm_location = payload.farm_location
        if payload.hardware_version:
            existing.hardware_version = payload.hardware_version
        db.commit()
        db.refresh(existing)
        return existing

    device = Device(
        device_mac=mac_clean,
        device_name=payload.device_name,
        farm_location=payload.farm_location,
        hardware_version=payload.hardware_version or "v2.0"
    )
    db.add(device)
    db.commit()
    db.refresh(device)
    return device

@router.get("", response_model=List[DeviceResponse])
def list_devices(db: Session = Depends(get_db)):
    return db.query(Device).order_by(Device.registered_at.desc()).all()
