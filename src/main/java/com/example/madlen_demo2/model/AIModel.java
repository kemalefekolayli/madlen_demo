package com.example.madlen_demo2.model;

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

    // Indicates if the model supports vision/image inputs
    @Builder.Default
    private boolean supportsVision = false;
}