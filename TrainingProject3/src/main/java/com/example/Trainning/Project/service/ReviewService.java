package com.example.Trainning.Project.service;

import com.example.Trainning.Project.dto.response.ApiResponse;
import com.example.Trainning.Project.dto.Review.ReviewRequest;
import com.example.Trainning.Project.dto.Review.ReviewResponse;
import com.example.Trainning.Project.model.Customer;
import com.example.Trainning.Project.model.Review;
import com.example.Trainning.Project.model.ServiceSession;
import com.example.Trainning.Project.repository.CustomerRepository;
import com.example.Trainning.Project.repository.ReviewRepository;
import com.example.Trainning.Project.repository.ServiceSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ServiceSessionRepository serviceSessionRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Transactional
    public ApiResponse<ReviewResponse> saveOrUpdateReview(ReviewRequest request) {
        Long customerId = request.getCustomerId();
        if (customerId == null) {
            return new ApiResponse<>(false, "Không thể xác định thông tin khách hàng.", null, HttpStatus.UNAUTHORIZED.value());
        }

        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null) {
            return new ApiResponse<>(false, "Không tìm thấy khách hàng với ID: " + customerId, null, HttpStatus.NOT_FOUND.value());
        }

        ServiceSession serviceSession = serviceSessionRepository.findById(request.getServiceSessionId()).orElse(null);
        if (serviceSession == null) {
            return new ApiResponse<>(false, "Không tìm thấy phiên dịch vụ với ID: " + request.getServiceSessionId(), null, HttpStatus.NOT_FOUND.value());
        }

        Long sessionCustomerId = serviceSession.getVehicle().getCustomer().getId();
        if (!customerId.equals(sessionCustomerId)) {
            return new ApiResponse<>(false, "Chỉ chủ xe mới có quyền đánh giá phiên sửa chữa này.", null, HttpStatus.FORBIDDEN.value());
        }

        Optional<Review> optionalReview = reviewRepository.findByServiceSession_Id(request.getServiceSessionId());

        Review review;
        boolean isUpdate = false;

        if (optionalReview.isPresent()) {
            review = optionalReview.get();

            if (!review.getCustomer().getId().equals(customerId)) {
                return new ApiResponse<>(false, "Bạn không có quyền chỉnh sửa đánh giá này.", null, HttpStatus.FORBIDDEN.value());
            }

            review.setUpdatedAt(LocalDateTime.now());
            isUpdate = true;

        } else {
            review = new Review();
            review.setCustomer(customer);
            review.setServiceSession(serviceSession);
            review.setCreatedAt(LocalDateTime.now());
            serviceSession.setReview(review);
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        review = reviewRepository.save(review);

        if (!isUpdate) {
            serviceSessionRepository.save(serviceSession);
        }

        ReviewResponse response = new ReviewResponse(
                review.getId(),
                review.getServiceSession().getId(),
                review.getRating(),
                review.getComment(),
                review.getCustomer().getFullName(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );

        String message = isUpdate ? "Cập nhật đánh giá thành công!" : "Thêm đánh giá thành công!";
        return new ApiResponse<>(true, message, response, HttpStatus.OK.value());
    }
}