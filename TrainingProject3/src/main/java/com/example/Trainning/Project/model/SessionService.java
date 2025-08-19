package com.example.Trainning.Project.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "session_services")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionService {

    @EmbeddedId
    private SessionServiceId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("serviceSessionId")
    @JoinColumn(name = "service_session_id", nullable = false)
    private ServiceSession serviceSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("serviceId")
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(name = "cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal cost;

    public SessionService(ServiceSession serviceSession, Service service, BigDecimal cost) {
        this.serviceSession = serviceSession;
        this.service = service;
        this.cost = cost;
        this.id = new SessionServiceId(serviceSession.getId(), service.getId());
    }
}