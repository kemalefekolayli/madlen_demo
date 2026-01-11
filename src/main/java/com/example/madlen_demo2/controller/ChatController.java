package com.example.madlen_demo2.controller;

import com.example.madlen_demo2.dto.ChatRequest;
import com.example.madlen_demo2.dto.ChatResponse;
import com.example.madlen_demo2.dto.CreateSessionRequest;
import com.example.madlen_demo2.dto.SessionResponse;
import com.example.madlen_demo2.model.AIModel;
import com.example.madlen_demo2.service.ChatService;
import io.micrometer.observation.annotation.Observed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // ==================== Model Endpoints ====================

    /**
     * Get all available AI models
     * GET /api/models
     */
    @GetMapping("/models")
    @Observed(name = "api.get-models")
    public ResponseEntity<List<AIModel>> getModels() {
        log.debug("GET /api/models");
        return ResponseEntity.ok(chatService.getAvailableModels());
    }

    /**
     * Get only vision-capable AI models
     * GET /api/models/vision
     */
    @GetMapping("/models/vision")
    @Observed(name = "api.get-vision-models")
    public ResponseEntity<List<AIModel>> getVisionModels() {
        log.debug("GET /api/models/vision");
        return ResponseEntity.ok(chatService.getVisionCapableModels());
    }

    /**
     * Check if a specific model supports vision
     * GET /api/models/{modelId}/supports-vision
     */
    @GetMapping("/models/{modelId}/supports-vision")
    @Observed(name = "api.check-vision-support")
    public ResponseEntity<Map<String, Object>> checkVisionSupport(@PathVariable String modelId) {
        log.debug("GET /api/models/{}/supports-vision", modelId);
        boolean supportsVision = chatService.modelSupportsVision(modelId);
        return ResponseEntity.ok(Map.of(
                "modelId", modelId,
                "supportsVision", supportsVision
        ));
    }

    // ==================== Session Endpoints ====================

    /**
     * Create a new chat session
     * POST /api/sessions
     */
    @PostMapping("/sessions")
    @Observed(name = "api.create-session")
    public ResponseEntity<SessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
        log.debug("POST /api/sessions - userId: {}, model: {}", request.getUserId(), request.getModel());
        SessionResponse session = chatService.createSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    /**
     * Get all sessions for a user
     * GET /api/sessions?userId={userId}
     */
    @GetMapping("/sessions")
    @Observed(name = "api.get-sessions")
    public ResponseEntity<List<SessionResponse>> getSessions(@RequestParam String userId) {
        log.debug("GET /api/sessions - userId: {}", userId);
        return ResponseEntity.ok(chatService.getUserSessions(userId));
    }

    /**
     * Get a specific session with message history
     * GET /api/sessions/{sessionId}
     */
    @GetMapping("/sessions/{sessionId}")
    @Observed(name = "api.get-session")
    public ResponseEntity<SessionResponse> getSession(@PathVariable String sessionId) {
        log.debug("GET /api/sessions/{}", sessionId);
        return ResponseEntity.ok(chatService.getSession(sessionId));
    }

    /**
     * Delete a session
     * DELETE /api/sessions/{sessionId}?userId={userId}
     */
    @DeleteMapping("/sessions/{sessionId}")
    @Observed(name = "api.delete-session")
    public ResponseEntity<Void> deleteSession(
            @PathVariable String sessionId,
            @RequestParam String userId) {
        log.debug("DELETE /api/sessions/{} - userId: {}", sessionId, userId);
        chatService.deleteSession(sessionId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Update session model
     * PATCH /api/sessions/{sessionId}/model
     */
    @PatchMapping("/sessions/{sessionId}/model")
    @Observed(name = "api.update-model")
    public ResponseEntity<SessionResponse> updateSessionModel(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> body) {
        log.debug("PATCH /api/sessions/{}/model", sessionId);
        String newModel = body.get("model");
        return ResponseEntity.ok(chatService.updateSessionModel(sessionId, newModel));
    }

    // ==================== Chat Endpoints ====================

    /**
     * Send a message and get response (non-streaming)
     * Supports multi-modal messages with images
     * POST /api/chat
     *
     * Request body example with images:
     * {
     *   "sessionId": "...",
     *   "message": "What's in this image?",
     *   "images": [
     *     {
     *       "type": "base64",
     *       "data": "base64_encoded_image_data",
     *       "mediaType": "image/jpeg"
     *     }
     *   ]
     * }
     */
    @PostMapping("/chat")
    @Observed(name = "api.chat")
    public ResponseEntity<ChatResponse> sendMessage(@Valid @RequestBody ChatRequest request) {
        log.debug("POST /api/chat - sessionId: {}, hasImages: {}",
                request.getSessionId(), request.hasImages());
        return ResponseEntity.ok(chatService.sendMessage(request));
    }

    /**
     * Send a message and stream the response
     * Supports multi-modal messages with images
     * POST /api/chat/stream
     * Returns Server-Sent Events
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Observed(name = "api.chat-stream")
    public Flux<String> sendMessageStream(@Valid @RequestBody ChatRequest request) {
        log.debug("POST /api/chat/stream - sessionId: {}, hasImages: {}",
                request.getSessionId(), request.hasImages());
        return chatService.sendMessageStream(request);
    }

    /**
     * Get message history for a session (alias for getSession)
     * GET /api/history/{sessionId}
     */
    @GetMapping("/history/{sessionId}")
    @Observed(name = "api.get-history")
    public ResponseEntity<SessionResponse> getHistory(@PathVariable String sessionId) {
        log.debug("GET /api/history/{}", sessionId);
        return ResponseEntity.ok(chatService.getSession(sessionId));
    }

    // ==================== Health Check ====================

    /**
     * Simple health check endpoint
     * GET /api/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "madlen-chat",
                "features", "multi-modal-vision"
        ));
    }
}