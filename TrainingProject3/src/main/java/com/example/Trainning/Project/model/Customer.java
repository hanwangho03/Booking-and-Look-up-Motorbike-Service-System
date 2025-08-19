package com.example.Trainning.Project.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.*; // Import các annotation validation

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên đầy đủ không được để trống")
    @Size(max = 100, message = "Tên đầy đủ không được vượt quá 100 ký tự")
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
    @Column(name = "email", unique = true, length = 100)
    private String email;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    @NotNull(message = "Thông tin người dùng (User) không được để trống")
    private User user;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Set<Vehicle> vehicles = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}