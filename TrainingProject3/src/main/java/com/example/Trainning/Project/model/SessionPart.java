package com.example.Trainning.Project.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "session_parts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionPart {

    @EmbeddedId
    private SessionPartId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("serviceSessionId")
    @JoinColumn(name = "service_session_id", nullable = false)
    private ServiceSession serviceSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("partId")
    @JoinColumn(name = "part_id", nullable = false)
    private Part part;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    public SessionPart(ServiceSession serviceSession, Part part, Integer quantity, BigDecimal unitPrice) {
        this.serviceSession = serviceSession;
        this.part = part;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.id = new SessionPartId(serviceSession.getId(), part.getId());
    }
}