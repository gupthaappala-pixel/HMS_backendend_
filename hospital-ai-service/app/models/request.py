from pydantic import BaseModel
from typing import List, Optional

class MedicationItem(BaseModel):
    medicineName: str = ""
    dosage: str = ""
    frequency: str = ""
    durationDays: int = 0

class PatientProfile(BaseModel):
    firstName: str
    lastName: str
    allergies: Optional[str] = None
    activeMedicines: List[dict] = []

class ChatRequest(BaseModel):
    message: str
    patientProfile: Optional[PatientProfile] = None
