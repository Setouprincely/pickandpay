package com.doch.Logistic.service.impl;

import com.doch.Fleet.model.Vehicle;
import com.doch.Fleet.repository.VehicleRepository;
import com.doch.Logistic.dto.DeliveryMapper;
import com.doch.Logistic.dto.DeliveryResponseDTO;
import com.doch.Logistic.dto.LocationDTO;
import com.doch.Logistic.dto.driver.DriverLocationBroadcastDTO;
import com.doch.Logistic.dto.driver.DriverPerformanceDTO;
import com.doch.Logistic.dto.driver.DriverProfileDTO;
import com.doch.Logistic.dto.driver.UpdateAvailabilityDTO;
import com.doch.Logistic.model.Delivery;
import com.doch.Logistic.model.DeliveryStatus;
import com.doch.Logistic.repository.DeliveryRepository;
import com.doch.Logistic.service.DriverService;
import com.doch.User.model.DriverProfile;
import com.doch.User.model.User;
import com.doch.User.repository.DriverProfileRepository;
import com.doch.User.repository.UserRepository;
import com.doch.common.exceptions.BusinessException;
import com.doch.common.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private static final List<DeliveryStatus> FINISHED = List.of(DeliveryStatus.DELIVERED, DeliveryStatus.CANCELLED, DeliveryStatus.RETURNED);

    private final UserRepository userRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final DeliveryRepository deliveryRepository;
    private final VehicleRepository vehicleRepository;
    private final DeliveryMapper deliveryMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public void updateAvailability(User driver, UpdateAvailabilityDTO request) {
        if (!driver.hasRole("ROLE_DELIVERY")) {
            throw new AccessDeniedException("Only users with the 'ROLE_DELIVERY' role can update availability.");
        }
        if (Boolean.FALSE.equals(request.getIsAvailable())) {
            long active = deliveryRepository.findByDriverAndStatusNotIn(driver, FINISHED).stream()
                    .filter(d -> d.getStatus() != DeliveryStatus.DRIVER_ASSIGNED).count();
            if (active > 0) {
                throw new BusinessException("You still carry " + active + " package(s). Deliver them or bring them to a hub before going offline.");
            }
        }
        driver.setIsAvailable(request.getIsAvailable());
        userRepository.save(driver);
        log.info("Driver {} is now {}", driver.getEmail(), Boolean.TRUE.equals(request.getIsAvailable()) ? "ONLINE" : "OFFLINE");
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryResponseDTO> getAssignedDeliveries(User driver) {
        return deliveryRepository.findByDriverAndStatusNotIn(driver, FINISHED).stream()
                .sorted(Comparator.comparing(d -> d.getAssignedAt() != null ? d.getAssignedAt() : LocalDateTime.MIN))
                .map(d -> deliveryMapper.toDto(d, DeliveryMapper.Perspective.DRIVER, true))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryResponseDTO> getDeliveryHistory(User driver) {
        return deliveryRepository.findByDriverAndStatusNotIn(driver, List.of(
                        DeliveryStatus.DRIVER_ASSIGNED, DeliveryStatus.PICKED_UP, DeliveryStatus.IN_TRANSIT,
                        DeliveryStatus.FAILED, DeliveryStatus.PENDING_ASSIGNMENT, DeliveryStatus.PENDING_PAYMENT,
                        DeliveryStatus.PENDING_PREPARATION, DeliveryStatus.AT_HUB)).stream()
                .sorted(Comparator.comparing((Delivery d) -> d.getEndTime() != null ? d.getEndTime() : LocalDateTime.MIN).reversed())
                .map(d -> deliveryMapper.toDto(d, DeliveryMapper.Perspective.DRIVER, false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DriverProfileDTO getMyProfile(User driver) {
        return mapToProfileDto(driver, driverProfileRepository.findByUser(driver).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverProfileDTO> getAllDrivers() {
        return userRepository.findAll().stream()
                .filter(u -> u.hasRole("ROLE_DELIVERY"))
                .map(user -> mapToProfileDto(user, driverProfileRepository.findByUser(user).orElse(null)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverProfileDTO> getAvailableDrivers() {
        return userRepository.findAll().stream()
                .filter(u -> u.hasRole("ROLE_DELIVERY")
                        && u.isEnabled() && Boolean.TRUE.equals(u.getIsAvailable()))
                .map(user -> mapToProfileDto(user, driverProfileRepository.findByUser(user).orElse(null)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DriverProfileDTO getDriverProfile(String driverId) {
        User user = userRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver user not found"));
        DriverProfile profile = driverProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for this user"));
        return mapToProfileDto(user, profile);
    }

    @Override
    @Transactional
    public void updateCurrentLocation(User driver, LocationDTO location) {
        DriverProfile profile = driverProfileRepository.findByUser(driver)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found. Please complete onboarding."));
        profile.setLastLatitude(location.getLatitude());
        profile.setLastLongitude(location.getLongitude());
        profile.setLastLocationUpdate(LocalDateTime.now());
        driverProfileRepository.save(profile);

        messagingTemplate.convertAndSend("/topic/driver-locations", DriverLocationBroadcastDTO.builder()
                .driverId(driver.getId())
                .driverName(driver.getFirstName() + " " + driver.getLastName())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .timestamp(LocalDateTime.now().toString())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public DriverPerformanceDTO getPerformanceStats(User driver) {
        List<Delivery> delivered = deliveryRepository.findByDriverAndStatus(driver, DeliveryStatus.DELIVERED);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        long today = delivered.stream().filter(d -> d.getEndTime() != null && !d.getEndTime().isBefore(startOfToday)).count();
        long week = delivered.stream().filter(d -> d.getEndTime() != null && d.getEndTime().isAfter(now.minusWeeks(1))).count();
        long month = delivered.stream().filter(d -> d.getEndTime() != null && d.getEndTime().isAfter(now.minusMonths(1))).count();
        int active = deliveryRepository.findByDriverAndStatusNotIn(driver, FINISHED).size();
        Double rating = driverProfileRepository.findByUser(driver).map(DriverProfile::getRating).orElse(null);

        List<DriverPerformanceDTO.RecentTask> recent = delivered.stream()
                .filter(d -> d.getEndTime() != null)
                .sorted(Comparator.comparing(Delivery::getEndTime).reversed())
                .limit(5)
                .map(d -> DriverPerformanceDTO.RecentTask.builder()
                        .deliveryId(d.getId())
                        .trackingNumber(d.getTrackingNumber())
                        .completedAt(d.getEndTime().toString())
                        .destination(d.getDropoffAddress())
                        .build())
                .toList();

        return DriverPerformanceDTO.builder()
                .completedToday((int) today)
                .completedThisWeek((int) week)
                .completedThisMonth((int) month)
                .activeJobs(active)
                .rating(rating)
                .recentTaskHistory(recent)
                .build();
    }

    private DriverProfileDTO mapToProfileDto(User user, DriverProfile profile) {
        Vehicle vehicle = profile != null ? vehicleRepository.findByAssignedDriverId(profile.getId()).orElse(null) : null;
        int active = deliveryRepository.findByDriverAndStatusNotIn(user, FINISHED).size();
        return DriverProfileDTO.builder()
                .driverId(user.getId())
                .profileId(profile != null ? profile.getId() : null)
                .fullName(user.getFirstName() + " " + user.getLastName())
                .email(user.getEmail())
                .phone(user.getTel())
                .licenseNumber(profile != null ? profile.getLicenseNumber() : null)
                .vehicleType(vehicle != null ? vehicle.getVehicleType() : (profile != null ? profile.getVehicleType() : null))
                .plateNumber(vehicle != null ? vehicle.getPlateNumber() : (profile != null ? profile.getPlateNumber() : null))
                .vehicleId(vehicle != null ? vehicle.getId() : null)
                .vehicleName(vehicle != null ? vehicle.getName() : null)
                .vehicleCategory(vehicle != null && vehicle.getCategory() != null ? vehicle.getCategory().name() : null)
                .rating(profile != null ? profile.getRating() : null)
                .totalDeliveries(profile != null ? profile.getTotalDeliveries() : 0)
                .activeDeliveries(active)
                .isAvailable(Boolean.TRUE.equals(user.getIsAvailable()))
                .build();
    }
}