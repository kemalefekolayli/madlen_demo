package com.example.madlen_demo2.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    private String role; // "user" or "assistant"
    private String content;

    // For multi-modal messages - list of images attached to this message
    private List<ImageContent> images;

    private String model;

    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Check if this message has images attached
     */
    public boolean hasImages() {
        return images != null && !images.isEmpty();
    }

    /**
     * Check if this is a multi-modal message
     */
    public boolean isMultiModal() {
        return hasImages() && "user".equals(role);
    }
}