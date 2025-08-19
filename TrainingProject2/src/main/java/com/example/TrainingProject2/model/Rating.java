package com.example.TrainingProject2.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.w3c.dom.Text;

import java.time.LocalDateTime;

@Entity
@Table(name = "ratings")
@Data
@NoArgsConstructor
public class Rating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, columnDefinition = "INT UNSIGNED")
    private User user;

    @OneToOne
    @JoinColumn(name = "appointment_id", unique = true, nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Appointment appointment;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
