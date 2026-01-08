package com.example.madlen_demo2.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_sessions")
public class ChatSession {
    
    @Id
    private String id;
    
    @Indexed
    private String userId;
    
    private String title;
    
    private String selectedModel;
    
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();
    
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @Builder.Default
    private Instant updatedAt = Instant.now();
    
    public void addMessage(ChatMessage message) {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }
        this.messages.add(message);
        this.updatedAt = Instant.now();
        
        // Auto-generate title from first user message if not set
        if (this.title == null && "user".equals(message.getRole())) {
            this.title = generateTitle(message.getContent());
        }
    }
    
    private String generateTitle(String content) {
        if (content == null || content.isBlank()) {
            return "New Chat";
        }
        // Take first 50 chars or until newline
        String title = content.split("\n")[0];
        if (title.length() > 50) {
            title = title.substring(0, 47) + "...";
        }
        return title;
    }
}
