from datetime import datetime, timezone
from sqlalchemy import Column, Integer, BigInteger, String, Float, DateTime
from ..database import Base

class Inspection(Base):
    __tablename__ = "inspections"

    id = Column(Integer, primary_key=True, index=True)
    device_mac = Column(String(32), index=True, nullable=False)
    seq_id = Column(Integer, nullable=True)
    timestamp = Column(BigInteger, index=True, nullable=False)
    crop_name = Column(String(64), default="Paddy", nullable=False)
    mode = Column(String(64), default="Storage Check", nullable=False)
    moisture = Column(Float, nullable=False)
    t1_top = Column(Float, nullable=False)
    t2_middle = Column(Float, nullable=False)
    t3_bottom = Column(Float, nullable=False)
    ambient_humidity = Column(Float, default=65.0)
    status = Column(String(32), default="SAFE", nullable=False)
    risk_score = Column(Integer, default=0)
    dominant_risk = Column(String(256), nullable=True)
    recommendations = Column(String(1024), nullable=True)
    created_at = Column(DateTime, default=lambda: datetime.now(timezone.utc))
