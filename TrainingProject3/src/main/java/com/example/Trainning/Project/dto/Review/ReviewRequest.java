// src/main/java/com/example/Trainning/Project/dto.review/ReviewRequest.java
package com.example.Trainning.Project.dto.Review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewRequest {
    private Long id;
    @NotNull(message = "ID của phiên dịch vụ không được để trống")
    private Long serviceSessionId;

    @NotNull(message = "Điểm đánh giá không được để trống")
    @Min(value = 1, message = "Điểm đánh giá phải từ 1 đến 5")
    @Max(value = 5, message = "Điểm đánh giá phải từ 1 đến 5")
    private Integer rating;

    @Size(max = 500, message = "Bình luận không được vượt quá 500 ký tự")
    private String comment;
    private Long customerId;
}