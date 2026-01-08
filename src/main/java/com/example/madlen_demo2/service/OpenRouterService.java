package com.example.madlen_demo2.service;



import com.example.madlen_demo2.config.OpenRouterProperties;
import com.example.madlen_demo2.exception.ChatExceptions;
import com.example.madlen_demo2.model.AIModel;
import com.example.madlen_demo2.model.ChatMessage;
import com.example.madlen_demo2.dto.OpenRouterDtos.*;
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
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenRouterService {
    
    private final WebClient openRouterWebClient;
    private final OpenRouterProperties properties;
    
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
     * Send a chat completion request (non-streaming)
     */
    @Observed(name = "openrouter.chat-completion")
    public ChatMessage sendChatRequest(String model, List<ChatMessage> history, String userMessage) {
        validateApiKey();
        
        log.info("Sending chat request to model: {}", model);
        
        // Build messages list
        List<Message> messages = buildMessageList(history, userMessage);
        
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
                    .timeout(Duration.ofSeconds(60))
                    .block();
            
            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new ChatExceptions.OpenRouterException("Empty response from AI model");
            }
            
            String assistantContent = response.getChoices().get(0).getMessage().getContent();
            
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
            if (e instanceof ChatExceptions.OpenRouterException) {
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
        validateApiKey();
        
        log.info("Sending streaming chat request to model: {}", model);
        
        List<Message> messages = buildMessageList(history, userMessage);
        
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
                .timeout(Duration.ofSeconds(120))
                .filter(line -> line != null && !line.isBlank() && !line.equals("[DONE]"))
                .map(this::extractContentFromStreamChunk)
                .filter(content -> content != null && !content.isEmpty())
                .onErrorMap(e -> {
                    log.error("Streaming error: {}", e.getMessage());
                    return new ChatExceptions.OpenRouterException("Streaming failed: " + e.getMessage());
                });
    }
    
    private List<Message> buildMessageList(List<ChatMessage> history, String userMessage) {
        List<Message> messages = history.stream()
                .map(m -> Message.builder()
                        .role(m.getRole())
                        .content(m.getContent())
                        .build())
                .collect(Collectors.toList());
        
        // Add the new user message
        messages.add(Message.builder()
                .role("user")
                .content(userMessage)
                .build());
        
        return messages;
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
