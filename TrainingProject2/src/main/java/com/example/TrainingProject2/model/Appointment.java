package com.example.TrainingProject2.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_user_id", nullable = false)
    private User customer;

    @ManyToOne
    @JoinColumn(name = "technician_user_id", nullable = false)
    private User technician;

    @ManyToOne
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceFix service;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private Status status;


    @OneToOne(mappedBy = "appointment" , cascade = CascadeType.ALL, orphanRemoval = true)
    private Rating rating;

    public enum Status {
        da_xac_nhan, da_hoan_thanh, da_huy, cho_xac_nhan
    }
}