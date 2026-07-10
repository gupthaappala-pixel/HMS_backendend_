import os
import json
from typing import List

class RagService:
    def __init__(self):
        # Locate knowledge_base.json in the parent project directory
        self.kb_path = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), "knowledge_base.json")
        self.corpus = []
        self.load_corpus()

    def load_corpus(self):
        try:
            if os.path.exists(self.kb_path):
                with open(self.kb_path, "r", encoding="utf-8") as f:
                    self.corpus = json.load(f)
                print(f"RAG Service loaded {len(self.corpus)} document chunks successfully.")
            else:
                print(f"RAG Knowledge base not found at {self.kb_path}")
        except Exception as e:
            print(f"Failed to load RAG knowledge base: {e}")

    def retrieve(self, query: str, top_k: int = 2) -> List[str]:
        if not self.corpus:
            return []

        # Token overlap TF-IDF simulation for robust keyword retrieval
        query_tokens = set(query.lower().replace("?", "").replace(".", "").replace(",", "").split())
        scores = []
        for doc in self.corpus:
            content = doc.get("content", "")
            doc_tokens = set(content.lower().split())
            overlap = len(query_tokens.intersection(doc_tokens))
            scores.append((overlap, content))

        # Sort by overlap descending
        scores.sort(key=lambda x: x[0], reverse=True)
        return [content for score, content in scores[:top_k] if score > 0]

rag_service = RagService()
