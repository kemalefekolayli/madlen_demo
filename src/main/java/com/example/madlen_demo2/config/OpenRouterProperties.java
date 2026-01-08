package com.example.madlen_demo2.config;

import com.example.madlen_demo2.model.AIModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import com.example.madlen_demo2.model.AIModel;
@Data
@Configuration
@ConfigurationProperties(prefix = "openrouter")
public class OpenRouterProperties {
    
    private Api api = new Api();
    private List<AIModel> freeModels = new ArrayList<>();
    
    @Data
    public static class Api {
        private String baseUrl = "https://openrouter.ai/api/v1";
        private String key;
    }
}
