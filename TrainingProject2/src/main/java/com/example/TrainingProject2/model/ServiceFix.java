package com.example.TrainingProject2.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

@Entity
@Table(name = "services")
@Data
@NoArgsConstructor
public class ServiceFix {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    @Column(nullable = false)
    private int estimatedDurationMinutes;

    @OneToMany(mappedBy = "service")
    private Set<Appointment> appointments;
}