package com.doch.Fleet.repository;

import com.doch.Fleet.model.Vehicle;
import com.doch.Fleet.model.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, String> {

    Optional<Vehicle> findByPlateNumber(String plateNumber);

    List<Vehicle> findByStatus(VehicleStatus status);

    Optional<Vehicle> findByAssignedDriverId(String driverProfileId);
}
