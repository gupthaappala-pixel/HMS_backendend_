from app.models.request import PatientProfile
from typing import List

class PromptService:
    def build_system_prompt(self, patient_profile: PatientProfile = None) -> str:
        prompt = (
            "You are Qwen, a state-of-the-art virtual medical assistant for Antigravity General Hospital.\n"
            "Your role is to assist patient portal users with general health inquiries, hospital department locations, FAQs, and medication directions.\n"
            "RULES:\n"
            "1. You are NOT a doctor. You must not diagnose conditions or recommend direct medical treatments.\n"
            "2. Suggest consulting a licensed medical practitioner for clinical diagnoses.\n"
            "3. Keep answers concise, clear, and formatted in markdown.\n\n"
        )
        if patient_profile:
            prompt += (
                f"Active Patient: {patient_profile.firstName} {patient_profile.lastName}\n"
                f"Known Allergies: {patient_profile.allergies if patient_profile.allergies else 'None reported'}\n"
            )
            if patient_profile.activeMedicines:
                prompt += "Current Prescribed Medications:\n"
                for item in patient_profile.activeMedicines:
                    name = item.get("name", item.get("medicineName", "Medicine"))
                    dosage = item.get("dosage", "N/A")
                    freq = item.get("frequency", "N/A")
                    days = item.get("durationDays", "N/A")
                    prompt += f"- {name} (Dosage: {dosage}, Freq: {freq}, Duration: {days} days)\n"
            prompt += "\n"
        return prompt

    def build_user_prompt(self, query: str, contexts: List[str]) -> str:
        prompt = ""
        if contexts:
            prompt += "Relevant Hospital Context:\n"
            for ctx in contexts:
                prompt += f"- {ctx}\n"
            prompt += "\n"
        prompt += f"Patient Query: \"{query}\"\n"
        return prompt

prompt_service = PromptService()
