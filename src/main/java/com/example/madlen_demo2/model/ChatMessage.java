package com.madlen.chat.model;

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
    
    @Builder.Default
    private Instant timestamp = Instant.now();
}
