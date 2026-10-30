package com.doch.Fleet.service;

import com.doch.Fleet.dto.VehicleRequestDTO;
import com.doch.Fleet.dto.VehicleResponseDTO;

import java.util.List;

public interface FleetService {

    VehicleResponseDTO createVehicle(VehicleRequestDTO request);

    VehicleResponseDTO updateVehicle(String vehicleId, VehicleRequestDTO request);

    VehicleResponseDTO getVehicle(String vehicleId);

    List<VehicleResponseDTO> getAllVehicles(String status);

    void deleteVehicle(String vehicleId);

    VehicleResponseDTO assignDriver(String vehicleId, String driverProfileId);

    VehicleResponseDTO unassignDriver(String vehicleId);

    VehicleResponseDTO updateStatus(String vehicleId, String status);
}
