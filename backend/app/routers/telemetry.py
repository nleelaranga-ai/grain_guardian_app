from typing import Optional
from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.orm import Session
from ..database import get_db
from ..models.inspection import Inspection
from ..models.risk_event import RiskEvent
from ..schemas.telemetry import (
    TelemetrySyncRequest,
    TelemetrySyncResponse,
    TelemetryLatestResponse
)

router = APIRouter(prefix="/telemetry", tags=["Telemetry"])

@router.post("/sync", response_model=TelemetrySyncResponse, status_code=status.HTTP_201_CREATED)
def sync_telemetry(payload: TelemetrySyncRequest, db: Session = Depends(get_db)):
    mac_clean = payload.device_mac.strip().upper()
    inserted_count = 0
    skipped_count = 0

    for rec in payload.records:
        # Check temporal deduplication (+- 30 seconds)
        window_start = rec.timestamp - 30000
        window_end = rec.timestamp + 30000

        existing = db.query(Inspection).filter(
            Inspection.device_mac == mac_clean,
            Inspection.timestamp >= window_start,
            Inspection.timestamp <= window_end
        ).first()

        if existing:
            skipped_count += 1
            continue

        inspection = Inspection(
            device_mac=mac_clean,
            seq_id=rec.seq_id,
            timestamp=rec.timestamp,
            crop_name=rec.crop_name,
            mode=rec.mode,
            moisture=rec.moisture,
            t1_top=rec.t1_top,
            t2_middle=rec.t2_middle,
            t3_bottom=rec.t3_bottom,
            ambient_humidity=rec.ambient_humidity or 65.0,
            status=rec.status,
            risk_score=rec.risk_score or 0,
            dominant_risk=rec.dominant_risk,
            recommendations=rec.recommendations
        )
        db.add(inspection)
        inserted_count += 1

        # Automated Hotspot & Risk Event Trigger
        t_max = max(rec.t1_top, rec.t2_middle, rec.t3_bottom)
        t_min = min(rec.t1_top, rec.t2_middle, rec.t3_bottom)
        tgi = round(t_max - t_min, 2)

        if tgi >= 3.0 or t_max >= 35.0 or rec.moisture >= 15.0 or rec.status in ("WARNING", "HIGH_RISK"):
            is_high_risk = tgi >= 4.0 or t_max >= 38.0 or rec.moisture >= 16.5 or rec.status == "HIGH_RISK"
            event = RiskEvent(
                device_mac=mac_clean,
                crop_name=rec.crop_name,
                tgi=tgi,
                peak_temp=t_max,
                status="HIGH_RISK" if is_high_risk else "WARNING",
                headline_en="HIGH RISK - URGENT ACTION REQUIRED" if is_high_risk else "WARNING - THERMAL STRATIFICATION DETECTED",
                headline_te="అత్యధిక ప్రమాదం - వెంటనే సరిచేయండి" if is_high_risk else "హెచ్చరిక - ధాన్యంలో వేడి మరియు తేమ పెరుగుతోంది",
                acknowledged=False
            )
            db.add(event)

    db.commit()

    return {
        "inserted": inserted_count,
        "duplicates_skipped": skipped_count,
        "status": "sync_complete"
    }

@router.get("/latest", response_model=TelemetryLatestResponse)
def get_latest_telemetry(
    device_mac: Optional[str] = Query(None, description="MAC address of the probe"),
    db: Session = Depends(get_db)
):
    query = db.query(Inspection)
    if device_mac:
        query = query.filter(Inspection.device_mac == device_mac.strip().upper())

    latest = query.order_by(Inspection.timestamp.desc()).first()
    if not latest:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No telemetry records found"
        )

    return {
        "device_mac": latest.device_mac,
        "crop_name": latest.crop_name,
        "moisture": latest.moisture,
        "t1_top": latest.t1_top,
        "t2_middle": latest.t2_middle,
        "t3_bottom": latest.t3_bottom,
        "ambient_humidity": latest.ambient_humidity,
        "status": latest.status,
        "risk_score": latest.risk_score,
        "dominant_risk": latest.dominant_risk,
        "timestamp": latest.timestamp
    }
