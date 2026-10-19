from .health import router as health_router
from .auth import router as auth_router
from .devices import router as devices_router
from .telemetry import router as telemetry_router
from .storage import router as storage_router
from .farms import router as farms_router

__all__ = [
    "health_router",
    "auth_router",
    "devices_router",
    "telemetry_router",
    "storage_router",
    "farms_router"
]
