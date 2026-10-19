from typing import Optional, List
from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.orm import Session
from sqlalchemy import func
from ..database import get_db
from ..models.inspection import Inspection
from ..models.risk_event import RiskEvent
from ..schemas.telemetry import (
    TelemetrySyncRequest,
    TelemetrySyncResponse,
    TelemetryLatestResponse,
    TelemetryHistoryResponse,
    TelemetryHistoryItem,
    TelemetrySummaryResponse
)

router = APIRouter(prefix="/telemetry", tags=["Telemetry"])

@router.post("/sync", response_model=TelemetrySyncResponse, status_code=status.HTTP_201_CREATED)
def sync_telemetry(payload: TelemetrySyncRequest, db: Session = Depends(get_db)):
    mac_clean = payload.device_mac.strip().upper()
    inserted_count = 0
    skipped_count = 0
    synced_uuids: List[str] = []

    for rec in payload.records:
        rec_uuid = rec.uuid or rec.sync_uuid
        existing = None

        # 1. Primary Deduplication: Strict UUID Matching
        if rec_uuid:
            existing = db.query(Inspection).filter(
                Inspection.uuid == rec_uuid
            ).first()

        # 2. Fallback Deduplication: Temporal Window (+- 30 seconds for same probe)
        if not existing:
            window_start = rec.timestamp - 30000
            window_end = rec.timestamp + 30000
            existing = db.query(Inspection).filter(
                Inspection.device_mac == mac_clean,
                Inspection.timestamp >= window_start,
                Inspection.timestamp <= window_end
            ).first()

        if existing:
            skipped_count += 1
            if rec_uuid:
                synced_uuids.append(rec_uuid)
            continue

        inspection = Inspection(
            uuid=rec_uuid,
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
        if rec_uuid:
            synced_uuids.append(rec_uuid)

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
        "status": "sync_complete",
        "synced_uuids": synced_uuids
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

@router.get("/history", response_model=TelemetryHistoryResponse)
def get_telemetry_history(
    device_mac: Optional[str] = Query(None, description="Filter by device MAC"),
    crop_name: Optional[str] = Query(None, description="Filter by crop name"),
    status_filter: Optional[str] = Query(None, alias="status", description="Filter by SAFE, WARNING, or HIGH_RISK"),
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db)
):
    query = db.query(Inspection)
    if device_mac:
        query = query.filter(Inspection.device_mac == device_mac.strip().upper())
    if crop_name:
        query = query.filter(Inspection.crop_name.ilike(f"%{crop_name.strip()}%"))
    if status_filter:
        query = query.filter(Inspection.status == status_filter.strip().upper())

    total = query.count()
    offset = (page - 1) * size
    records = query.order_by(Inspection.timestamp.desc()).offset(offset).limit(size).all()

    items = [
        TelemetryHistoryItem(
            id=r.id,
            uuid=r.uuid,
            device_mac=r.device_mac,
            seq_id=r.seq_id,
            timestamp=r.timestamp,
            crop_name=r.crop_name,
            mode=r.mode,
            moisture=r.moisture,
            t1_top=r.t1_top,
            t2_middle=r.t2_middle,
            t3_bottom=r.t3_bottom,
            ambient_humidity=r.ambient_humidity,
            status=r.status,
            risk_score=r.risk_score,
            dominant_risk=r.dominant_risk,
            recommendations=r.recommendations
        )
        for r in records
    ]

    return {
        "total": total,
        "page": page,
        "size": size,
        "records": items
    }

@router.get("/summary", response_model=TelemetrySummaryResponse)
def get_telemetry_summary(
    device_mac: Optional[str] = Query(None, description="Optional MAC address filter"),
    db: Session = Depends(get_db)
):
    query = db.query(Inspection)
    if device_mac:
        query = query.filter(Inspection.device_mac == device_mac.strip().upper())

    total = query.count()
    if total == 0:
        return {
            "total_inspections": 0,
            "avg_moisture": 0.0,
            "max_temperature": 0.0,
            "high_risk_count": 0,
            "warning_count": 0,
            "safe_count": 0,
            "latest_timestamp": None
        }

    avg_moisture = db.query(func.avg(Inspection.moisture)).scalar() or 0.0
    
    # Max temperature across all three depths
    max_t1 = db.query(func.max(Inspection.t1_top)).scalar() or 0.0
    max_t2 = db.query(func.max(Inspection.t2_middle)).scalar() or 0.0
    max_t3 = db.query(func.max(Inspection.t3_bottom)).scalar() or 0.0
    peak_temp = max(max_t1, max_t2, max_t3)

    high_risks = query.filter(Inspection.status == "HIGH_RISK").count()
    warnings = query.filter(Inspection.status == "WARNING").count()
    safes = query.filter(Inspection.status == "SAFE").count()
    latest_rec = query.order_by(Inspection.timestamp.desc()).first()

    return {
        "total_inspections": total,
        "avg_moisture": round(float(avg_moisture), 2),
        "max_temperature": round(float(peak_temp), 2),
        "high_risk_count": high_risks,
        "warning_count": warnings,
        "safe_count": safes,
        "latest_timestamp": latest_rec.timestamp if latest_rec else None
    }
