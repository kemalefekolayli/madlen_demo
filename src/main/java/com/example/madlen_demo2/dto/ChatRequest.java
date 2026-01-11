package com.example.madlen_demo2.dto;

import com.example.madlen_demo2.model.ImageContent;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotBlank(message = "Session ID is required")
    private String sessionId;

    @NotBlank(message = "Message content is required")
    private String message;

    private String model; // Optional - uses session's model if not provided

    // Optional - list of images to include with the message
    private List<ImageContent> images;

    /**
     * Check if this request includes images
     */
    public boolean hasImages() {
        return images != null && !images.isEmpty();
    }
}