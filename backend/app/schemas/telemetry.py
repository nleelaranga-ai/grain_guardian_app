from typing import List, Optional
from pydantic import BaseModel, Field

class TelemetryRecord(BaseModel):
    uuid: Optional[str] = None
    sync_uuid: Optional[str] = None
    seq_id: Optional[int] = None
    timestamp: int
    crop_name: str = "Paddy"
    mode: str = "Storage Check"
    moisture: float = Field(..., ge=0.0, le=50.0, description="Grain moisture percentage")
    t1_top: float = Field(..., ge=-40.0, le=100.0, description="Top layer temperature C")
    t2_middle: float = Field(..., ge=-40.0, le=100.0, description="Middle layer temperature C")
    t3_bottom: float = Field(..., ge=-40.0, le=100.0, description="Bottom layer temperature C")
    ambient_humidity: Optional[float] = Field(65.0, ge=0.0, le=100.0)
    status: str = "SAFE"
    risk_score: Optional[int] = Field(0, ge=0, le=100)
    dominant_risk: Optional[str] = "Normal condition"
    recommendations: Optional[str] = ""

class TelemetrySyncRequest(BaseModel):
    device_mac: str
    records: List[TelemetryRecord]

class TelemetrySyncResponse(BaseModel):
    inserted: int
    duplicates_skipped: int
    status: str
    synced_uuids: List[str] = []

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

class TelemetryHistoryItem(BaseModel):
    id: int
    uuid: Optional[str] = None
    device_mac: str
    seq_id: Optional[int] = None
    timestamp: int
    crop_name: str
    mode: str
    moisture: float
    t1_top: float
    t2_middle: float
    t3_bottom: float
    ambient_humidity: float
    status: str
    risk_score: int
    dominant_risk: Optional[str] = None
    recommendations: Optional[str] = None

class TelemetryHistoryResponse(BaseModel):
    total: int
    page: int
    size: int
    records: List[TelemetryHistoryItem]

class TelemetrySummaryResponse(BaseModel):
    total_inspections: int
    avg_moisture: float
    max_temperature: float
    high_risk_count: int
    warning_count: int
    safe_count: int
    latest_timestamp: Optional[int] = None
