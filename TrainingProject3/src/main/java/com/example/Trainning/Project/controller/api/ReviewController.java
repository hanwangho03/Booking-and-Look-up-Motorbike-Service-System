// src/main/java/com/example/Trainning/Project.controller/ReviewController.java
package com.example.Trainning.Project.controller.api;

import com.example.Trainning.Project.dto.response.ApiResponse;
import com.example.Trainning.Project.dto.Review.ReviewRequest;
import com.example.Trainning.Project.dto.Review.ReviewResponse;
import com.example.Trainning.Project.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    // Endpoint để thêm hoặc cập nhật đánh giá
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> saveOrUpdateReview(
            @Valid @RequestBody ReviewRequest request,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        String userToken = extractToken(authorizationHeader);

        ApiResponse<ReviewResponse> response = reviewService.saveOrUpdateReview(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }
}