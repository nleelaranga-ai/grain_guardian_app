from typing import List, Optional
from pydantic import BaseModel

class TelemetryRecord(BaseModel):
    seq_id: Optional[int] = None
    timestamp: int
    crop_name: str = "Paddy"
    mode: str = "Storage Check"
    moisture: float
    t1_top: float
    t2_middle: float
    t3_bottom: float
    ambient_humidity: Optional[float] = 65.0
    status: str = "SAFE"
    risk_score: Optional[int] = 0
    dominant_risk: Optional[str] = "Normal condition"
    recommendations: Optional[str] = ""

class TelemetrySyncRequest(BaseModel):
    device_mac: str
    records: List[TelemetryRecord]

class TelemetrySyncResponse(BaseModel):
    inserted: int
    duplicates_skipped: int
    status: str

class TelemetryLatestResponse(BaseModel):
    device_mac: str
    crop_name: str
    moisture: float
    t1_top: float
    t2_middle: float
    t3_bottom: float
    ambient_humidity: float
    status: str
    risk_score: int
    dominant_risk: Optional[str]
    timestamp: int
