package com.example.TrainingProject2.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String name;
    private String phoneNumber;
    private String email;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @OneToMany(mappedBy = "customer")
    private Set<Appointment> customerAppointments;

    @OneToMany(mappedBy = "technician")
    private Set<Appointment> technicianAppointments;
}