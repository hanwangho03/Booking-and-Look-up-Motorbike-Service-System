package com.example.TrainingProject2.repository;

import com.example.TrainingProject2.model.ServiceFix;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<ServiceFix, Integer> {
}