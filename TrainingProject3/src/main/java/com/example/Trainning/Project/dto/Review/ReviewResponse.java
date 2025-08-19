// src/main/java/com/example/Trainning/Project/dto/Review/ReviewResponse.java
package com.example.Trainning.Project.dto.Review;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long serviceSessionId;
    private Integer rating;
    private String comment;
    private String customerFullName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}