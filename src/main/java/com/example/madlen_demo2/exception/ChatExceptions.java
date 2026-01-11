package com.example.madlen_demo2.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class ChatExceptions {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class SessionNotFoundException extends RuntimeException {
        public SessionNotFoundException(String sessionId) {
            super("Chat session not found: " + sessionId);
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class SessionLimitExceededException extends RuntimeException {
        public SessionLimitExceededException(int limit) {
            super("Maximum session limit reached: " + limit + ". Please delete old sessions to create new ones.");
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class MessageLimitExceededException extends RuntimeException {
        public MessageLimitExceededException(int limit) {
            super("Maximum message limit reached for this session: " + limit + ". Please start a new session.");
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class InvalidModelException extends RuntimeException {
        public InvalidModelException(String model) {
            super("Invalid or unavailable model: " + model);
        }
    }

    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public static class OpenRouterException extends RuntimeException {
        public OpenRouterException(String message) {
            super("AI service error: " + message);
        }
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public static class ApiKeyNotConfiguredException extends RuntimeException {
        public ApiKeyNotConfiguredException() {
            super("OpenRouter API key is not configured. Please set OPENROUTER_API_KEY environment variable.");
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class VisionNotSupportedException extends RuntimeException {
        public VisionNotSupportedException(String model) {
            super("The selected model '" + model + "' does not support image/vision inputs. Please select a vision-capable model.");
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public static class InvalidImageException extends RuntimeException {
        public InvalidImageException(String reason) {
            super("Invalid image: " + reason);
        }
    }

    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public static class ImageTooLargeException extends RuntimeException {
        public ImageTooLargeException(long maxSizeBytes) {
            super("Image size exceeds maximum allowed size of " + (maxSizeBytes / 1024 / 1024) + " MB");
        }
    }
}