import os
from dotenv import load_dotenv

# Load environment variables from .env file
load_dotenv()

class Config:
    HF_TOKEN: str = os.getenv("HF_TOKEN", "")
    HF_MODEL: str = os.getenv("HF_MODEL", "Qwen/Qwen2.5-7B-Instruct")

settings = Config()
