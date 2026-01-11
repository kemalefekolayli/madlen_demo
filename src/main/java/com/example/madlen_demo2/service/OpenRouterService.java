package com.example.madlen_demo2.service;

import com.example.madlen_demo2.config.OpenRouterProperties;
import com.example.madlen_demo2.dto.OpenRouterDtos.*;
import com.example.madlen_demo2.exception.ChatExceptions;
import com.example.madlen_demo2.model.AIModel;
import com.example.madlen_demo2.model.ChatMessage;
import com.example.madlen_demo2.model.ImageContent;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenRouterService {

    private final WebClient openRouterWebClient;
    private final OpenRouterProperties properties;

    // Maximum image size in bytes (5 MB)
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    /**
     * Get list of available free models
     */
    @Observed(name = "openrouter.get-models")
    public List<AIModel> getAvailableModels() {
        return properties.getFreeModels();
    }

    /**
     * Check if a model is valid and available
     */
    public boolean isValidModel(String modelId) {
        return properties.getFreeModels().stream()
                .anyMatch(m -> m.getId().equals(modelId));
    }

    /**
     * Check if a model supports vision/image inputs
     */
    public boolean supportsVision(String modelId) {
        return properties.getFreeModels().stream()
                .filter(m -> m.getId().equals(modelId))
                .findFirst()
                .map(AIModel::isSupportsVision)
                .orElse(false);
    }

    /**
     * Send a chat completion request (non-streaming)
     * Supports both text-only and multi-modal (with images) messages
     */
    @Observed(name = "openrouter.chat-completion")
    public ChatMessage sendChatRequest(String model, List<ChatMessage> history, String userMessage) {
        return sendChatRequest(model, history, userMessage, null);
    }

    /**
     * Send a chat completion request with optional images (non-streaming)
     */
    @Observed(name = "openrouter.chat-completion-multimodal")
    public ChatMessage sendChatRequest(String model, List<ChatMessage> history,
                                       String userMessage, List<ImageContent> images) {
        validateApiKey();

        boolean hasImages = images != null && !images.isEmpty();

        // Validate vision support if images are provided
        if (hasImages && !supportsVision(model)) {
            throw new ChatExceptions.VisionNotSupportedException(model);
        }

        // Validate images
        if (hasImages) {
            validateImages(images);
        }

        log.info("Sending chat request to model: {}, with images: {}", model, hasImages);

        // Build messages list
        List<Message> messages = buildMessageList(history, userMessage, images);

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .stream(false)
                .maxTokens(2048)
                .temperature(0.7)
                .build();

        try {
            ChatCompletionResponse response = openRouterWebClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApi().getKey())
                    .header("HTTP-Referer", "http://localhost:8080")
                    .header("X-Title", "Madlen Chat")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatCompletionResponse.class)
                    .timeout(Duration.ofSeconds(90)) // Longer timeout for vision requests
                    .block();

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new ChatExceptions.OpenRouterException("Empty response from AI model");
            }

            Object contentObj = response.getChoices().get(0).getMessage().getContent();
            String assistantContent = contentObj != null ? contentObj.toString() : "";

            log.info("Received response from model: {}, tokens used: {}",
                    model, response.getUsage() != null ? response.getUsage().getTotalTokens() : "unknown");

            return ChatMessage.builder()
                    .role("assistant")
                    .content(assistantContent)
                    .build();

        } catch (WebClientResponseException e) {
            log.error("OpenRouter API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ChatExceptions.OpenRouterException("AI service returned error: " + e.getMessage());
        } catch (Exception e) {
            if (e instanceof ChatExceptions.OpenRouterException ||
                    e instanceof ChatExceptions.VisionNotSupportedException) {
                throw e;
            }
            log.error("Failed to communicate with OpenRouter", e);
            throw new ChatExceptions.OpenRouterException("Failed to communicate with AI service: " + e.getMessage());
        }
    }

    /**
     * Send a streaming chat completion request
     * Returns a Flux that emits content chunks as they arrive
     */
    @Observed(name = "openrouter.chat-completion-stream")
    public Flux<String> sendChatRequestStream(String model, List<ChatMessage> history, String userMessage) {
        return sendChatRequestStream(model, history, userMessage, null);
    }

    /**
     * Send a streaming chat completion request with optional images
     */
    @Observed(name = "openrouter.chat-completion-stream-multimodal")
    public Flux<String> sendChatRequestStream(String model, List<ChatMessage> history,
                                              String userMessage, List<ImageContent> images) {
        validateApiKey();

        boolean hasImages = images != null && !images.isEmpty();

        // Validate vision support if images are provided
        if (hasImages && !supportsVision(model)) {
            return Flux.error(new ChatExceptions.VisionNotSupportedException(model));
        }

        // Validate images
        if (hasImages) {
            try {
                validateImages(images);
            } catch (Exception e) {
                return Flux.error(e);
            }
        }

        log.info("Sending streaming chat request to model: {}, with images: {}", model, hasImages);

        List<Message> messages = buildMessageList(history, userMessage, images);

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(model)
                .messages(messages)
                .stream(true)
                .maxTokens(2048)
                .temperature(0.7)
                .build();

        return openRouterWebClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApi().getKey())
                .header("HTTP-Referer", "http://localhost:8080")
                .header("X-Title", "Madlen Chat")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(180)) // Longer timeout for vision streaming
                .filter(line -> line != null && !line.isBlank() && !line.equals("[DONE]"))
                .map(this::extractContentFromStreamChunk)
                .filter(content -> content != null && !content.isEmpty())
                .onErrorMap(e -> {
                    log.error("Streaming error: {}", e.getMessage());
                    return new ChatExceptions.OpenRouterException("Streaming failed: " + e.getMessage());
                });
    }

    /**
     * Build message list for OpenRouter API
     * Handles both text-only and multi-modal messages
     */
    private List<Message> buildMessageList(List<ChatMessage> history, String userMessage,
                                           List<ImageContent> images) {
        List<Message> messages = new ArrayList<>();

        // Add history messages
        for (ChatMessage m : history) {
            if (m.isMultiModal()) {
                // Build multi-modal content for historical messages with images
                messages.add(buildMultiModalMessage(m.getRole(), m.getContent(), m.getImages()));
            } else {
                // Simple text message
                messages.add(Message.builder()
                        .role(m.getRole())
                        .content(m.getContent())
                        .build());
            }
        }

        // Add the new user message
        if (images != null && !images.isEmpty()) {
            messages.add(buildMultiModalMessage("user", userMessage, images));
        } else {
            messages.add(Message.builder()
                    .role("user")
                    .content(userMessage)
                    .build());
        }

        return messages;
    }

    /**
     * Build a multi-modal message with text and images
     */
    private Message buildMultiModalMessage(String role, String text, List<ImageContent> images) {
        List<ContentPart> contentParts = new ArrayList<>();

        // Add text part first
        if (text != null && !text.isBlank()) {
            contentParts.add(ContentPart.builder()
                    .type("text")
                    .text(text)
                    .build());
        }

        // Add image parts
        for (ImageContent image : images) {
            String imageUrl;
            if ("base64".equals(image.getType())) {
                // Convert to data URI format
                imageUrl = "data:" + image.getMediaType() + ";base64," + image.getData();
            } else {
                // Use URL directly
                imageUrl = image.getData();
            }

            contentParts.add(ContentPart.builder()
                    .type("image_url")
                    .imageUrl(ImageUrl.builder()
                            .url(imageUrl)
                            .detail("auto")
                            .build())
                    .build());
        }

        return Message.builder()
                .role(role)
                .content(contentParts)
                .build();
    }

    /**
     * Validate images before sending to API
     */
    private void validateImages(List<ImageContent> images) {
        for (ImageContent image : images) {
            if (!image.isValid()) {
                throw new ChatExceptions.InvalidImageException("Invalid image format or missing data");
            }

            // Check base64 size (rough estimate)
            if ("base64".equals(image.getType())) {
                long estimatedSize = (long) (image.getData().length() * 0.75);
                if (estimatedSize > MAX_IMAGE_SIZE) {
                    throw new ChatExceptions.ImageTooLargeException(MAX_IMAGE_SIZE);
                }
            }
        }
    }

    private String extractContentFromStreamChunk(String chunk) {
        try {
            // SSE format: data: {"choices":[{"delta":{"content":"..."}}]}
            if (chunk.startsWith("data: ")) {
                chunk = chunk.substring(6);
            }

            if (chunk.equals("[DONE]") || chunk.isBlank()) {
                return "";
            }

            // Simple JSON parsing for delta content
            // Looking for "content":"..." in the delta object
            int deltaIndex = chunk.indexOf("\"delta\"");
            if (deltaIndex == -1) return "";

            int contentIndex = chunk.indexOf("\"content\"", deltaIndex);
            if (contentIndex == -1) return "";

            int colonIndex = chunk.indexOf(":", contentIndex);
            if (colonIndex == -1) return "";

            int startQuote = chunk.indexOf("\"", colonIndex + 1);
            if (startQuote == -1) return "";

            int endQuote = findClosingQuote(chunk, startQuote + 1);
            if (endQuote == -1) return "";

            String content = chunk.substring(startQuote + 1, endQuote);
            // Unescape common sequences
            return content
                    .replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");

        } catch (Exception e) {
            log.trace("Failed to parse stream chunk: {}", chunk);
            return "";
        }
    }

    private int findClosingQuote(String s, int start) {
        for (int i = start; i < s.length(); i++) {
            if (s.charAt(i) == '"' && (i == 0 || s.charAt(i - 1) != '\\')) {
                return i;
            }
        }
        return -1;
    }

    private void validateApiKey() {
        if (properties.getApi().getKey() == null || properties.getApi().getKey().isBlank()) {
            throw new ChatExceptions.ApiKeyNotConfiguredException();
        }
    }
}