from datetime import datetime
from typing import List
from pydantic import BaseModel, ConfigDict

class RiskEventResponse(BaseModel):
    event_id: int
    device_mac: str
    crop_name: str
    tgi: float
    peak_temp: float
    status: str
    headline_en: str
    headline_te: str
    created_at: datetime
    acknowledged: bool

    model_config = ConfigDict(from_attributes=True)

class RiskListResponse(BaseModel):
    total: int
    events: List[RiskEventResponse]
