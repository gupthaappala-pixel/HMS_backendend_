package com.hospital.chatbot;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000,https://hospitalmanagemen.health}")
public class AiChatController {

    private final MockLlmService aiChatService;

    public AiChatController(MockLlmService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('PATIENT','ADMIN','DOCTOR','NURSE','PHARMACIST')")
    public ResponseEntity<AiChatResponse> chat(
            @Valid @RequestBody AiChatRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        String responseText = aiChatService.processChat(userDetails.getUsername(), request.getMessage());
        return ResponseEntity.ok(new AiChatResponse(responseText));
    }

    @PostMapping(value = "/upload-report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('PATIENT','ADMIN','DOCTOR','NURSE','PHARMACIST','LAB_TECHNICIAN')")
    public ResponseEntity<AiChatResponse> uploadReport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "message", required = false) String message,
            @AuthenticationPrincipal UserDetails userDetails) {

        String responseText = aiChatService.processReportUpload(userDetails.getUsername(), file, message);
        return ResponseEntity.ok(new AiChatResponse(responseText));
    }
}
