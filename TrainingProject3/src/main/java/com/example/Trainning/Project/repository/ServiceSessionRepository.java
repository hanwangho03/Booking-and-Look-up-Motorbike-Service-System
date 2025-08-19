// src/main/java/com/example/Trainning/Project/repository/ServiceSessionRepository.java
package com.example.Trainning.Project.repository;

import com.example.Trainning.Project.model.ServiceSession;
import com.example.Trainning.Project.model.Vehicle;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServiceSessionRepository extends JpaRepository<ServiceSession, Long> {

    @EntityGraph(attributePaths = {"sessionServices.service", "sessionParts.part", "technician", "review.customer"})
    @Query("SELECT ss FROM ServiceSession ss WHERE ss.vehicle = :vehicle ORDER BY ss.sessionDate DESC")
    List<ServiceSession> findByVehicleOrderBySessionDateDesc(@Param("vehicle") Vehicle vehicle);

    @EntityGraph(attributePaths = {"technician", "review.customer"})
    Optional<ServiceSession> findTopByVehicleOrderBySessionDateDesc(Vehicle vehicle);

    @Query("SELECT ss FROM ServiceSession ss JOIN ss.sessionServices sss JOIN sss.service s " +
            "WHERE ss.vehicle = :vehicle AND s.name IN :maintenanceServiceNames " +
            "ORDER BY ss.sessionDate DESC")
    List<ServiceSession> findLatestMaintenanceSessionsByVehicleAndServiceNames(
            Vehicle vehicle, List<String> maintenanceServiceNames
    );

    @EntityGraph(attributePaths = {"vehicle.customer.user", "technician", "sessionServices.service", "sessionParts.part", "review.customer"})
    List<ServiceSession> findAllByOrderBySessionDateDesc();
}