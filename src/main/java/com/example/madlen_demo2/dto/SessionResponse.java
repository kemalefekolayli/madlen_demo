package com.example.madlen_demo2.dto;

import com.example.madlen_demo2.model.ChatMessage;
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
public class SessionResponse {
    
    private String id;
    private String userId;
    private String title;
    private String selectedModel;
    private List<ChatMessage> messages;
    private int messageCount;
    private Instant createdAt;
    private Instant updatedAt;
}
