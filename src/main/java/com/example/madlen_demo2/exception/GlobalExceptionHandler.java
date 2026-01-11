package com.example.madlen_demo2.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Data
    @Builder
    @AllArgsConstructor
    public static class ErrorResponse {
        private Instant timestamp;
        private int status;
        private String error;
        private String message;
        private Map<String, String> details;
    }

    @ExceptionHandler(ChatExceptions.SessionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSessionNotFound(ChatExceptions.SessionNotFoundException ex) {
        log.warn("Session not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ChatExceptions.SessionLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleSessionLimit(ChatExceptions.SessionLimitExceededException ex) {
        log.warn("Session limit exceeded: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ChatExceptions.MessageLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleMessageLimit(ChatExceptions.MessageLimitExceededException ex) {
        log.warn("Message limit exceeded: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ChatExceptions.InvalidModelException.class)
    public ResponseEntity<ErrorResponse> handleInvalidModel(ChatExceptions.InvalidModelException ex) {
        log.warn("Invalid model: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ChatExceptions.OpenRouterException.class)
    public ResponseEntity<ErrorResponse> handleOpenRouterError(ChatExceptions.OpenRouterException ex) {
        log.error("OpenRouter API error: {}", ex.getMessage());
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(ChatExceptions.ApiKeyNotConfiguredException.class)
    public ResponseEntity<ErrorResponse> handleApiKeyNotConfigured(ChatExceptions.ApiKeyNotConfiguredException ex) {
        log.error("API key not configured");
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(ChatExceptions.VisionNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleVisionNotSupported(ChatExceptions.VisionNotSupportedException ex) {
        log.warn("Vision not supported: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ChatExceptions.InvalidImageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidImage(ChatExceptions.InvalidImageException ex) {
        log.warn("Invalid image: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ChatExceptions.ImageTooLargeException.class)
    public ResponseEntity<ErrorResponse> handleImageTooLarge(ChatExceptions.ImageTooLargeException ex) {
        log.warn("Image too large: {}", ex.getMessage());
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: {}", errors);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .details(errors)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .build();

        return ResponseEntity.status(status).body(response);
    }
}