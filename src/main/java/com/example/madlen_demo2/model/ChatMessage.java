package com.example.madlen_demo2.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    
    private String role; // "user" or "assistant"
    private String content;

    private String model;
    @Builder.Default
    private Instant timestamp = Instant.now();
}
