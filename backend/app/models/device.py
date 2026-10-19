from datetime import datetime, timezone
from sqlalchemy import Column, Integer, String, DateTime, ForeignKey
from ..database import Base

class Device(Base):
    __tablename__ = "devices"

    id = Column(Integer, primary_key=True, index=True)
    device_mac = Column(String(32), unique=True, index=True, nullable=False)
    device_name = Column(String(64), nullable=False)
    farm_location = Column(String(256), nullable=False)
    hardware_version = Column(String(32), default="v2.0")
    user_id = Column(Integer, ForeignKey("users.id"), nullable=True)
    registered_at = Column(DateTime, default=lambda: datetime.now(timezone.utc))
