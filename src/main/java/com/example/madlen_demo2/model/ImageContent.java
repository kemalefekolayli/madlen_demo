package com.example.madlen_demo2.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageContent {

    private String type; // "base64" or "url"
    private String data; // base64 encoded image data or URL
    private String mediaType; // e.g., "image/jpeg", "image/png", "image/gif", "image/webp"

    /**
     * Create an ImageContent from base64 data
     */
    public static ImageContent fromBase64(String base64Data, String mediaType) {
        return ImageContent.builder()
                .type("base64")
                .data(base64Data)
                .mediaType(mediaType)
                .build();
    }

    /**
     * Create an ImageContent from URL
     */
    public static ImageContent fromUrl(String url) {
        return ImageContent.builder()
                .type("url")
                .data(url)
                .build();
    }

    /**
     * Check if this is a valid image content
     */
    public boolean isValid() {
        if (type == null || data == null || data.isBlank()) {
            return false;
        }

        if ("base64".equals(type)) {
            return mediaType != null && !mediaType.isBlank() && isValidMediaType(mediaType);
        }

        return "url".equals(type);
    }

    private boolean isValidMediaType(String mediaType) {
        return mediaType.equals("image/jpeg") ||
                mediaType.equals("image/png") ||
                mediaType.equals("image/gif") ||
                mediaType.equals("image/webp");
    }
}