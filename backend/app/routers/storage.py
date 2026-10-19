from typing import Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session
from ..database import get_db
from ..models.risk_event import RiskEvent
from ..schemas.risk import RiskListResponse, RiskEventResponse

router = APIRouter(prefix="/storage", tags=["Storage Risks"])

@router.get("/risks", response_model=RiskListResponse)
def get_storage_risks(
    status_filter: Optional[str] = Query(None, alias="status", description="WARNING or HIGH_RISK"),
    limit: int = Query(50, ge=1, le=100),
    offset: int = Query(0, ge=0),
    db: Session = Depends(get_db)
):
    query = db.query(RiskEvent)
    if status_filter:
        query = query.filter(RiskEvent.status == status_filter.strip().upper())

    total = query.count()
    events = query.order_by(RiskEvent.created_at.desc()).offset(offset).limit(limit).all()

    response_events = [
        RiskEventResponse(
            event_id=e.id,
            device_mac=e.device_mac,
            crop_name=e.crop_name,
            tgi=e.tgi,
            peak_temp=e.peak_temp,
            status=e.status,
            headline_en=e.headline_en,
            headline_te=e.headline_te,
            created_at=e.created_at,
            acknowledged=e.acknowledged
        )
        for e in events
    ]

    return {
        "total": total,
        "events": response_events
    }

@router.post("/risks/{event_id}/acknowledge")
def acknowledge_risk_event(event_id: int, db: Session = Depends(get_db)):
    event = db.query(RiskEvent).filter(RiskEvent.id == event_id).first()
    if not event:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Risk event not found")
    event.acknowledged = True
    db.commit()
    return {"status": "acknowledged", "event_id": event_id}
