package com.example.madlen_demo2.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "chat")
public class ChatProperties {
    
    private Session session = new Session();
    
    @Data
    public static class Session {
        private int maxPerUser = 10;
        private int maxMessagesPerSession = 100;
    }
}
