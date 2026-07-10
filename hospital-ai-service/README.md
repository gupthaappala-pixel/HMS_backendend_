# Antigravity Hospital AI Service

This is a dedicated Python AI microservice designed to handle real-time patient questions using **RAG (Retrieval-Augmented Generation)** and the **Hugging Face Serverless Inference API** running **Qwen 2.5 7B Instruct**.

---

## 🏗️ Architecture

- **Web Framework**: FastAPI (port 8000)
- **RAG Engine**: Token-overlap TF-IDF search retrieves layout configurations, FAQs, and medicine safety guidelines from `knowledge_base.json`.
- **LLM Router**: Proxies prompts to the Hugging Face Router endpoint running `Qwen/Qwen2.5-7B-Instruct`.
- **Fallback Engine**: Local context formatter outputs dynamic responses if the Hugging Face API is unreachable or has token limit errors.

---

## 🚀 Setup & Launch

1. **Install Python dependencies**:
   ```bash
   pip install -r requirements.txt
   ```

2. **Configure environment variables** in `.env`:
   ```env
   HF_TOKEN=hf_hSXqpqETZUrGilccO
   HF_MODEL=Qwen/Qwen2.5-7B-Instruct
   ```

3. **Start the FastAPI server**:
   ```bash
   python app/main.py
   ```
   The service will run locally at `http://localhost:8000`.

---

## 🔗 Endpoints

### 1. Chat Completion (`POST /chat`)
Expects standard message payloads matching the user specification:
```json
{
  "message": "Book an appointment with a cardiologist"
}
```
Response:
```json
{
  "reply": "Sure! Please choose your preferred date and time for the cardiology appointment.",
  "response": "Sure! Please choose your preferred date and time for the cardiology appointment."
}
```
