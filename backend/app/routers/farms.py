from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, ConfigDict
from sqlalchemy.orm import Session
from ..database import get_db
from ..models.farm import Farm

router = APIRouter(prefix="/farms", tags=["Farms"])

class FarmCreate(BaseModel):
    name: str
    location: str

class FarmResponse(BaseModel):
    id: int
    name: str
    location: str

    model_config = ConfigDict(from_attributes=True)

@router.get("", response_model=List[FarmResponse])
def get_farms(db: Session = Depends(get_db)):
    return db.query(Farm).all()

@router.post("", response_model=FarmResponse, status_code=status.HTTP_201_CREATED)
def create_farm(farm_in: FarmCreate, db: Session = Depends(get_db)):
    farm = Farm(name=farm_in.name, location=farm_in.location)
    db.add(farm)
    db.commit()
    db.refresh(farm)
    return farm
