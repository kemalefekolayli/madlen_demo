package com.example.madlen_demo2.dto;


import com.example.madlen_demo2.model.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    
    private String sessionId;
    private ChatMessage assistantMessage;
    private String model;
    private int totalMessages;
}
