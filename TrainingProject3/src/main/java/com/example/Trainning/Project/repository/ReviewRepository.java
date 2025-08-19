// src/main/java/com/example/Trainning/Project.repository/ReviewRepository.java
package com.example.Trainning.Project.repository;

import com.example.Trainning.Project.model.Review;
import com.example.Trainning.Project.model.ServiceSession;
import com.example.Trainning.Project.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByServiceSession_Id(Long serviceSessionId);

    Optional<Review> findByServiceSessionAndCustomer(ServiceSession serviceSession, Customer customer);

    boolean existsByServiceSession(ServiceSession session);

    List<Review> findByCustomer(Customer customer);

    List<Review> findByServiceSession_Technician_User_Id(Long technicianUserId);
}