package com.example.Trainning.Project.model;

import com.example.Trainning.Project.enums.ServiceSessionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "service_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private Technician technician;

    @Column(name = "session_date", nullable = false)
    private LocalDateTime sessionDate;

    @Column(name = "technician_notes", columnDefinition = "TEXT")
    private String technicianNotes;

    @Column(name = "total_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ServiceSessionStatus status = ServiceSessionStatus.completed;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "serviceSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SessionService> sessionServices = new HashSet<>();

    @OneToMany(mappedBy = "serviceSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SessionPart> sessionParts = new HashSet<>();

    // THÊM DÒNG NÀY VÀO ĐÂY
    @OneToOne(mappedBy = "serviceSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Review review;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceSession that = (ServiceSession) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}