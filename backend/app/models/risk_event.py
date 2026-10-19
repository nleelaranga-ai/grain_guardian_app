from datetime import datetime, timezone
from sqlalchemy import Column, Integer, String, Float, Boolean, DateTime
from ..database import Base

class RiskEvent(Base):
    __tablename__ = "risk_events"

    id = Column(Integer, primary_key=True, index=True)
    device_mac = Column(String(32), index=True, nullable=False)
    crop_name = Column(String(64), default="Paddy", nullable=False)
    tgi = Column(Float, nullable=False)
    peak_temp = Column(Float, nullable=False)
    status = Column(String(32), nullable=False)  # WARNING, HIGH_RISK
    headline_en = Column(String(256), nullable=False)
    headline_te = Column(String(256), nullable=False)
    acknowledged = Column(Boolean, default=False)
    created_at = Column(DateTime, default=lambda: datetime.now(timezone.utc))
