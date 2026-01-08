package com.madlen.chat.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIModel {
    
    private String id;
    private String name;
    private String description;
    
    @Builder.Default
    private boolean available = true;
}
