package com.example.madlen_demo2.service;

import com.example.madlen_demo2.config.ChatProperties;
import com.example.madlen_demo2.dto.ChatRequest;
import com.example.madlen_demo2.dto.ChatResponse;
import com.example.madlen_demo2.dto.CreateSessionRequest;
import com.example.madlen_demo2.dto.SessionResponse;
import com.example.madlen_demo2.exception.ChatExceptions;
import com.example.madlen_demo2.model.AIModel;
import com.example.madlen_demo2.model.ChatMessage;
import com.example.madlen_demo2.model.ChatSession;
import com.example.madlen_demo2.model.ImageContent;
import com.example.madlen_demo2.repository.ChatSessionRepository;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final OpenRouterService openRouterService;
    private final ChatProperties chatProperties;

    /**
     * Get all available AI models
     */
    public List<AIModel> getAvailableModels() {
        return openRouterService.getAvailableModels();
    }

    /**
     * Get only vision-capable models
     */
    public List<AIModel> getVisionCapableModels() {
        return openRouterService.getAvailableModels().stream()
                .filter(AIModel::isSupportsVision)
                .collect(Collectors.toList());
    }

    /**
     * Check if a model supports vision
     */
    public boolean modelSupportsVision(String modelId) {
        return openRouterService.supportsVision(modelId);
    }

    /**
     * Create a new chat session
     */
    @Observed(name = "chat.create-session")
    public SessionResponse createSession(CreateSessionRequest request) {
        log.info("Creating new session for user: {}", request.getUserId());

        // Validate model
        if (!openRouterService.isValidModel(request.getModel())) {
            throw new ChatExceptions.InvalidModelException(request.getModel());
        }

        // Check session limit
        long existingSessions = sessionRepository.countByUserId(request.getUserId());
        if (existingSessions >= chatProperties.getSession().getMaxPerUser()) {
            throw new ChatExceptions.SessionLimitExceededException(chatProperties.getSession().getMaxPerUser());
        }

        ChatSession session = ChatSession.builder()
                .userId(request.getUserId())
                .title(request.getTitle())
                .selectedModel(request.getModel())
                .build();

        session = sessionRepository.save(session);
        log.info("Created session: {} for user: {}", session.getId(), request.getUserId());

        return mapToSessionResponse(session);
    }

    /**
     * Get all sessions for a user
     */
    @Observed(name = "chat.get-sessions")
    public List<SessionResponse> getUserSessions(String userId) {
        log.debug("Fetching sessions for user: {}", userId);

        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(this::mapToSessionResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific session with full message history
     */
    @Observed(name = "chat.get-session")
    public SessionResponse getSession(String sessionId) {
        ChatSession session = findSessionOrThrow(sessionId);
        return mapToSessionResponse(session);
    }

    /**
     * Delete a session
     */
    @Observed(name = "chat.delete-session")
    public void deleteSession(String sessionId, String userId) {
        log.info("Deleting session: {} for user: {}", sessionId, userId);

        ChatSession session = findSessionOrThrow(sessionId);
        if (!session.getUserId().equals(userId)) {
            throw new ChatExceptions.SessionNotFoundException(sessionId);
        }

        sessionRepository.delete(session);
        log.info("Deleted session: {}", sessionId);
    }

    /**
     * Send a message and get a response (non-streaming)
     * Supports multi-modal messages with images
     */
    @Observed(name = "chat.send-message")
    public ChatResponse sendMessage(ChatRequest request) {
        log.info("Processing message for session: {}, has images: {}",
                request.getSessionId(), request.hasImages());

        ChatSession session = findSessionOrThrow(request.getSessionId());

        // Check message limit
        if (session.getMessages().size() >= chatProperties.getSession().getMaxMessagesPerSession()) {
            throw new ChatExceptions.MessageLimitExceededException(
                    chatProperties.getSession().getMaxMessagesPerSession());
        }

        // Use request model if provided, otherwise use session's model
        String model = request.getModel() != null ? request.getModel() : session.getSelectedModel();
        if (!openRouterService.isValidModel(model)) {
            throw new ChatExceptions.InvalidModelException(model);
        }

        // Validate vision support if images are provided
        List<ImageContent> images = request.getImages();
        if (request.hasImages() && !openRouterService.supportsVision(model)) {
            throw new ChatExceptions.VisionNotSupportedException(model);
        }

        // Add user message to history (including images if present)
        ChatMessage userMessage = ChatMessage.builder()
                .role("user")
                .content(request.getMessage())
                .images(images)
                .build();
        session.addMessage(userMessage);

        // Get AI response (with images if present)
        ChatMessage assistantMessage = openRouterService.sendChatRequest(
                model,
                session.getMessages().subList(0, session.getMessages().size() - 1),
                request.getMessage(),
                images
        );
        assistantMessage.setModel(model);

        // Add assistant response to history
        session.addMessage(assistantMessage);

        // Update model if changed
        if (request.getModel() != null) {
            session.setSelectedModel(model);
        }

        // Save session
        session = sessionRepository.save(session);

        log.info("Message processed for session: {}, total messages: {}",
                session.getId(), session.getMessages().size());

        return ChatResponse.builder()
                .sessionId(session.getId())
                .assistantMessage(assistantMessage)
                .model(model)
                .totalMessages(session.getMessages().size())
                .build();
    }

    /**
     * Send a message and stream the response
     * Supports multi-modal messages with images
     */
    @Observed(name = "chat.send-message-stream")
    public Flux<String> sendMessageStream(ChatRequest request) {
        log.info("Processing streaming message for session: {}, has images: {}",
                request.getSessionId(), request.hasImages());

        ChatSession session = findSessionOrThrow(request.getSessionId());

        // Check message limit
        if (session.getMessages().size() >= chatProperties.getSession().getMaxMessagesPerSession()) {
            return Flux.error(new ChatExceptions.MessageLimitExceededException(
                    chatProperties.getSession().getMaxMessagesPerSession()));
        }

        String model = request.getModel() != null ? request.getModel() : session.getSelectedModel();
        if (!openRouterService.isValidModel(model)) {
            return Flux.error(new ChatExceptions.InvalidModelException(model));
        }

        // Validate vision support if images are provided
        List<ImageContent> images = request.getImages();
        if (request.hasImages() && !openRouterService.supportsVision(model)) {
            return Flux.error(new ChatExceptions.VisionNotSupportedException(model));
        }

        // Add user message (including images if present)
        ChatMessage userMessage = ChatMessage.builder()
                .role("user")
                .content(request.getMessage())
                .images(images)
                .build();
        session.addMessage(userMessage);

        // Save session with user message
        ChatSession savedSession = sessionRepository.save(session);
        String sessionId = savedSession.getId();

        // Create a StringBuilder to accumulate the response
        StringBuilder fullResponse = new StringBuilder();

        return openRouterService.sendChatRequestStream(
                        model,
                        savedSession.getMessages().subList(0, savedSession.getMessages().size() - 1),
                        request.getMessage(),
                        images
                )
                .doOnNext(fullResponse::append)
                .doOnComplete(() -> {
                    // Save the complete assistant message after streaming is done
                    ChatSession currentSession = sessionRepository.findById(sessionId).orElse(null);
                    if (currentSession != null) {
                        ChatMessage assistantMessage = ChatMessage.builder()
                                .role("assistant")
                                .content(fullResponse.toString())
                                .model(model)
                                .build();
                        currentSession.addMessage(assistantMessage);
                        sessionRepository.save(currentSession);
                        log.info("Streaming complete for session: {}, saved {} chars",
                                sessionId, fullResponse.length());
                    }
                })
                .doOnError(e -> log.error("Streaming failed for session: {}", sessionId, e));
    }

    /**
     * Update session model
     */
    @Observed(name = "chat.update-model")
    public SessionResponse updateSessionModel(String sessionId, String newModel) {
        if (!openRouterService.isValidModel(newModel)) {
            throw new ChatExceptions.InvalidModelException(newModel);
        }

        ChatSession session = findSessionOrThrow(sessionId);
        session.setSelectedModel(newModel);
        session.setUpdatedAt(Instant.now());
        session = sessionRepository.save(session);

        log.info("Updated model for session: {} to: {}", sessionId, newModel);

        return mapToSessionResponse(session);
    }

    private ChatSession findSessionOrThrow(String sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ChatExceptions.SessionNotFoundException(sessionId));
    }

    private SessionResponse mapToSessionResponse(ChatSession session) {
        return SessionResponse.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .title(session.getTitle())
                .selectedModel(session.getSelectedModel())
                .messages(session.getMessages())
                .messageCount(session.getMessages() != null ? session.getMessages().size() : 0)
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}