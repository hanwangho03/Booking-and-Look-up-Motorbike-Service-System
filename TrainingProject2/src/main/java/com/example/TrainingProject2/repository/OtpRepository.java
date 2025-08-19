package com.example.TrainingProject2.repository;

import com.example.TrainingProject2.model.Otp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository <Otp, Long>{

    Optional<Otp> findByEmail(String email);
    void deleteByEmail(String email);

}
