from datetime import datetime
from typing import Optional
from pydantic import BaseModel, ConfigDict

class DeviceRegisterRequest(BaseModel):
    device_mac: str
    device_name: str
    farm_location: str
    hardware_version: Optional[str] = "v2.0"

class DeviceResponse(BaseModel):
    id: int
    device_mac: str
    device_name: str
    farm_location: str
    hardware_version: str
    registered_at: datetime

    model_config = ConfigDict(from_attributes=True)
