package com.doch.Fleet.service.impl;

import com.doch.Fleet.dto.VehicleRequestDTO;
import com.doch.Fleet.dto.VehicleResponseDTO;
import com.doch.Fleet.model.Vehicle;
import com.doch.Fleet.model.VehicleStatus;
import com.doch.Fleet.repository.VehicleRepository;
import com.doch.Fleet.service.FleetService;
import com.doch.User.model.DriverProfile;
import com.doch.User.repository.DriverProfileRepository;
import com.doch.common.exceptions.BusinessException;
import com.doch.common.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FleetServiceImpl implements FleetService {

    private final VehicleRepository vehicleRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final com.doch.common.audit.AuditService auditService;

    @Override
    @Transactional
    public VehicleResponseDTO createVehicle(VehicleRequestDTO request) {
        vehicleRepository.findByPlateNumber(request.getPlateNumber()).ifPresent(v -> {
            throw new BusinessException("A vehicle with plate number " + request.getPlateNumber() + " already exists.");
        });

        Vehicle vehicle = Vehicle.builder()
                .name(request.getName())
                .vehicleType(request.getVehicleType())
                .category(parseCategory(request.getCategory(), true))
                .maxLoadKg(request.getMaxLoadKg())
                .plateNumber(request.getPlateNumber())
                .make(request.getMake())
                .model(request.getModel())
                .year(request.getYear())
                .status(parseStatus(request.getStatus(), VehicleStatus.ACTIVE))
                .lastMaintenanceDate(request.getLastMaintenanceDate())
                .nextMaintenanceDate(request.getNextMaintenanceDate())
                .notes(request.getNotes())
                .build();

        if (StringUtils.hasText(request.getAssignedDriverProfileId())) {
            vehicle.setAssignedDriver(resolveDriver(request.getAssignedDriverProfileId()));
        }

        vehicleRepository.save(vehicle);
        log.info("FLEET: Vehicle created: {} ({})", vehicle.getName(), vehicle.getPlateNumber());
        auditService.record("VEHICLE_CREATED", "VEHICLE", vehicle.getId(), vehicle.getName() + " " + vehicle.getPlateNumber() + " " + vehicle.getCategory());
        return VehicleResponseDTO.fromVehicle(vehicle);
    }

    @Override
    @Transactional
    public VehicleResponseDTO updateVehicle(String vehicleId, VehicleRequestDTO request) {
        Vehicle vehicle = findVehicle(vehicleId);

        // Prevent duplicate plate numbers on update
        if (StringUtils.hasText(request.getPlateNumber())
                && !request.getPlateNumber().equals(vehicle.getPlateNumber())) {
            vehicleRepository.findByPlateNumber(request.getPlateNumber()).ifPresent(v -> {
                throw new BusinessException("A vehicle with plate number " + request.getPlateNumber() + " already exists.");
            });
            vehicle.setPlateNumber(request.getPlateNumber());
        }

        if (request.getName() != null) vehicle.setName(request.getName());
        if (request.getVehicleType() != null) vehicle.setVehicleType(request.getVehicleType());
        if (request.getCategory() != null) vehicle.setCategory(parseCategory(request.getCategory(), true));
        if (request.getMaxLoadKg() != null) vehicle.setMaxLoadKg(request.getMaxLoadKg());
        if (request.getMake() != null) vehicle.setMake(request.getMake());
        if (request.getModel() != null) vehicle.setModel(request.getModel());
        if (request.getYear() != null) vehicle.setYear(request.getYear());
        if (request.getStatus() != null) vehicle.setStatus(parseStatus(request.getStatus(), vehicle.getStatus()));
        if (request.getLastMaintenanceDate() != null) vehicle.setLastMaintenanceDate(request.getLastMaintenanceDate());
        if (request.getNextMaintenanceDate() != null) vehicle.setNextMaintenanceDate(request.getNextMaintenanceDate());
        if (request.getNotes() != null) vehicle.setNotes(request.getNotes());

        if (StringUtils.hasText(request.getAssignedDriverProfileId())) {
            vehicle.setAssignedDriver(resolveDriver(request.getAssignedDriverProfileId()));
        }

        vehicleRepository.save(vehicle);
        return VehicleResponseDTO.fromVehicle(vehicle);
    }

    @Override
    public VehicleResponseDTO getVehicle(String vehicleId) {
        return VehicleResponseDTO.fromVehicle(findVehicle(vehicleId));
    }

    @Override
    public List<VehicleResponseDTO> getAllVehicles(String status) {
        List<Vehicle> vehicles;
        if (StringUtils.hasText(status)) {
            vehicles = vehicleRepository.findByStatus(parseStatus(status, null));
        } else {
            vehicles = vehicleRepository.findAll();
        }
        return vehicles.stream().map(VehicleResponseDTO::fromVehicle).toList();
    }

    @Override
    @Transactional
    public void deleteVehicle(String vehicleId) {
        Vehicle vehicle = findVehicle(vehicleId);
        vehicle.setAssignedDriver(null);
        vehicle.setIsDeleted(true);
        vehicleRepository.save(vehicle);
        log.info("FLEET: Vehicle soft-deleted: {}", vehicleId);
        auditService.record("VEHICLE_DELETED", "VEHICLE", vehicleId, vehicle.getName());
    }

    @Override
    @Transactional
    public VehicleResponseDTO assignDriver(String vehicleId, String driverProfileId) {
        Vehicle vehicle = findVehicle(vehicleId);
        DriverProfile driver = resolveDriver(driverProfileId);

        // A driver can only be assigned to one vehicle at a time
        vehicleRepository.findByAssignedDriverId(driverProfileId).ifPresent(existing -> {
            if (!existing.getId().equals(vehicleId)) {
                throw new BusinessException("This driver is already assigned to vehicle: "
                        + existing.getName() + " (" + existing.getPlateNumber() + "). Unassign them first.");
            }
        });

        vehicle.setAssignedDriver(driver);
        vehicleRepository.save(vehicle);
        return VehicleResponseDTO.fromVehicle(vehicle);
    }

    @Override
    @Transactional
    public VehicleResponseDTO unassignDriver(String vehicleId) {
        Vehicle vehicle = findVehicle(vehicleId);
        vehicle.setAssignedDriver(null);
        vehicleRepository.save(vehicle);
        return VehicleResponseDTO.fromVehicle(vehicle);
    }

    @Override
    @Transactional
    public VehicleResponseDTO updateStatus(String vehicleId, String status) {
        Vehicle vehicle = findVehicle(vehicleId);
        vehicle.setStatus(parseStatus(status, null));
        vehicleRepository.save(vehicle);
        return VehicleResponseDTO.fromVehicle(vehicle);
    }

    // --- Helpers ---

    private Vehicle findVehicle(String vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));
    }

    private DriverProfile resolveDriver(String driverProfileId) {
        return driverProfileRepository.findById(driverProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found: " + driverProfileId));
    }

    private com.doch.Fleet.model.VehicleCategory parseCategory(String raw, boolean required) {
        if (!StringUtils.hasText(raw)) {
            if (required) throw new BusinessException("Vehicle category is required: BICYCLE, K_TRUCK or VAN.");
            return null;
        }
        try {
            return com.doch.Fleet.model.VehicleCategory.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid vehicle category: " + raw + ". Allowed values: BICYCLE, K_TRUCK, VAN");
        }
    }

    private VehicleStatus parseStatus(String raw, VehicleStatus fallback) {
        if (!StringUtils.hasText(raw)) {
            if (fallback != null) return fallback;
            throw new BusinessException("Vehicle status is required.");
        }
        try {
            return VehicleStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid vehicle status: " + raw
                    + ". Allowed values: ACTIVE, IN_MAINTENANCE, INACTIVE, RETIRED");
        }
    }
}
