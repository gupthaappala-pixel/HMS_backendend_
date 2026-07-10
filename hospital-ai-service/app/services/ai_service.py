from openai import OpenAI
from app.config import settings
from app.services.prompt_service import prompt_service
from app.services.rag_service import rag_service
from app.models.request import PatientProfile

class AiService:
    def __init__(self):
        self.client = None
        self.init_client()

    def init_client(self):
        token = settings.HF_TOKEN
        if token and not token.startswith("hf_token_placeholder"):
            try:
                self.client = OpenAI(
                    base_url="https://api-inference.huggingface.co/v1/",
                    api_key=token
                )
                print(f"Hugging Face OpenAI-compatible client initialized for model: {settings.HF_MODEL}")
            except Exception as e:
                print(f"Failed to initialize Hugging Face client: {e}")
        else:
            print("No valid HF_TOKEN found. Operating in local fallback mode.")

    def generate_chat_response(self, message: str, patient_profile: PatientProfile = None) -> str:
        # 1. Retrieve RAG context
        contexts = rag_service.retrieve(message)

        # 2. Build system and user prompts
        system_msg = prompt_service.build_system_prompt(patient_profile)
        user_msg = prompt_service.build_user_prompt(message, contexts)

        # 3. Request inference from HF model
        if self.client:
            try:
                completion = self.client.chat.completions.create(
                    model=settings.HF_MODEL,
                    messages=[
                        {"role": "system", "content": system_msg},
                        {"role": "user", "content": user_msg}
                    ],
                    max_tokens=400,
                    temperature=0.7
                )
                if completion.choices and completion.choices[0].message.content:
                    return completion.choices[0].message.content.strip()
            except Exception as e:
                print(f"Hugging Face Router API call failed: {e}. Falling back to local search.")

        # 4. Fallback Dynamic Generator
        if contexts:
            ctx = contexts[0]
            return (
                f"### 🤖 Antigravity AI (Local Context)\n\n"
                f"Here is the relevant hospital information I retrieved for you:\n\n{ctx}\n\n"
                f"*Please consult our hospital reception desk or a doctor for clinical assistance.*"
            )

        return (
            "### 🤖 Antigravity AI Assistant\n\n"
            "Hello! I am here to help you explain prescriptions, find active doctors, navigate hospital wards, and answer FAQs.\n\n"
            "Feel free to ask questions like: *'Where is the ICU?'* or *'What is Paracetamol?'*."
        )

ai_service = AiService()
