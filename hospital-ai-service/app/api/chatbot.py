from fastapi import APIRouter
from app.models.request import ChatRequest
from app.models.response import ChatResponse
from app.services.ai_service import ai_service

router = APIRouter()

@router.post("/chat", response_model=ChatResponse)
async def chat_endpoint(request: ChatRequest):
    reply_text = ai_service.generate_chat_response(request.message, request.patientProfile)
    return ChatResponse(reply=reply_text, response=reply_text)

@router.post("/api/ai/chat", response_model=ChatResponse)
async def chat_api_endpoint(request: ChatRequest):
    reply_text = ai_service.generate_chat_response(request.message, request.patientProfile)
    return ChatResponse(reply=reply_text, response=reply_text)
