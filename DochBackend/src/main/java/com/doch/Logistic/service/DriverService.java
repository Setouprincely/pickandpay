package com.doch.Logistic.service;

import com.doch.Logistic.dto.DeliveryResponseDTO;
import com.doch.Logistic.dto.LocationDTO;
import com.doch.Logistic.dto.driver.DriverPerformanceDTO;
import com.doch.Logistic.dto.driver.DriverProfileDTO;
import com.doch.Logistic.dto.driver.UpdateAvailabilityDTO;
import com.doch.User.model.User;

import java.util.List;

public interface DriverService {

    void updateAvailability(User driver, UpdateAvailabilityDTO request);

    /** Active jobs: assigned, collected, out for delivery or failed (package still with the driver). */
    List<DeliveryResponseDTO> getAssignedDeliveries(User driver);

    /** Finished jobs of the driver (delivered / returned), newest first. */
    List<DeliveryResponseDTO> getDeliveryHistory(User driver);

    DriverProfileDTO getMyProfile(User driver);

    List<DriverProfileDTO> getAllDrivers();

    List<DriverProfileDTO> getAvailableDrivers();

    DriverProfileDTO getDriverProfile(String driverId);

    void updateCurrentLocation(User driver, LocationDTO location);

    DriverPerformanceDTO getPerformanceStats(User driver);
}