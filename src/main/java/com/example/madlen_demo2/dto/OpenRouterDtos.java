package com.example.madlen_demo2.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class OpenRouterDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatCompletionRequest {
        private String model;
        private List<Message> messages;
        private boolean stream;

        @JsonProperty("max_tokens")
        private Integer maxTokens;

        private Double temperature;
    }

    /**
     * Message can have either:
     * - Simple string content (for text-only messages)
     * - Array of content parts (for multi-modal messages with images)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Message {
        private String role;

        // For simple text messages - this will be a string
        // For multi-modal messages - this will be null and contentParts will be used
        private Object content;
    }

    /**
     * Content part for multi-modal messages
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContentPart {
        private String type; // "text" or "image_url"

        // For text content
        private String text;

        // For image content
        @JsonProperty("image_url")
        private ImageUrl imageUrl;
    }

    /**
     * Image URL structure for OpenRouter API
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageUrl {
        private String url; // Can be a URL or base64 data URI

        // Optional - hint for image detail level
        private String detail; // "auto", "low", or "high"
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatCompletionResponse {
        private String id;
        private String object;
        private long created;
        private String model;
        private List<Choice> choices;
        private Usage usage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {
        private int index;
        private Message message;

        @JsonProperty("finish_reason")
        private String finishReason;

        // For streaming
        private Message delta;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private int promptTokens;

        @JsonProperty("completion_tokens")
        private int completionTokens;

        @JsonProperty("total_tokens")
        private int totalTokens;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StreamChunk {
        private String id;
        private String object;
        private long created;
        private String model;
        private List<Choice> choices;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorResponse {
        private Error error;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Error {
        private String message;
        private String type;
        private String code;
    }
}