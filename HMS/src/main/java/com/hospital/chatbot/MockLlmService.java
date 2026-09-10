package com.hospital.chatbot;

public interface MockLlmService {
    String processChat(String username, String userMessage);
    String processReportUpload(String username, org.springframework.web.multipart.MultipartFile file, String userMessage);
}
