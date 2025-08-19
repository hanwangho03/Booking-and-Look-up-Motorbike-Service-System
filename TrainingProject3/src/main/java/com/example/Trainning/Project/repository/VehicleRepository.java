// src/main/java/com/example/Trainning/Project/repository/VehicleRepository.java
package com.example.Trainning.Project.repository;

import com.example.Trainning.Project.model.ServiceSession;
import com.example.Trainning.Project.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    // Existing methods
    Optional<Vehicle> findByLicensePlate(String licensePlate);
    List<Vehicle> findByCustomer_User_PhoneNumber(String phoneNumber);

    @Query("SELECT v FROM Vehicle v JOIN FETCH v.customer c JOIN FETCH c.user u WHERE v.licensePlate = :licensePlate")
    Optional<Vehicle> findByLicensePlateWithCustomerAndUser(@Param("licensePlate") String licensePlate);

    @Query("SELECT v FROM Vehicle v JOIN FETCH v.customer c JOIN FETCH c.user u WHERE c.user.phoneNumber = :phoneNumber")
    List<Vehicle> findByCustomer_User_PhoneNumberWithCustomerAndUser(@Param("phoneNumber") String phoneNumber);

    @Query("SELECT v FROM Vehicle v JOIN FETCH v.customer c JOIN FETCH c.user u")
    List<Vehicle> findAllVehiclesWithCustomerAndUser();

    @Query("SELECT ss FROM ServiceSession ss WHERE ss.vehicle = :vehicle ORDER BY ss.sessionDate DESC")
    List<ServiceSession> findByVehicleOrderBySessionDateDesc(@Param("vehicle") Vehicle vehicle);

    @Query("SELECT ss FROM ServiceSession ss WHERE ss.vehicle = :vehicle ORDER BY ss.sessionDate DESC LIMIT 1")
    Optional<ServiceSession> findTopByVehicleOrderBySessionDateDesc(@Param("vehicle") Vehicle vehicle);
}