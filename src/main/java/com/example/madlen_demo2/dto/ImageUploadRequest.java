package com.example.madlen_demo2.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadRequest {

    @NotBlank(message = "Base64 image data is required")
    private String imageData; // Base64 encoded image data (without data URI prefix)

    @NotBlank(message = "Media type is required")
    private String mediaType; // e.g., "image/jpeg", "image/png"

    private String filename; // Optional original filename
}