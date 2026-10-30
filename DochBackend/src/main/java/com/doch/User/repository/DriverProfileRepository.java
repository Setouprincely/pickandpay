package com.doch.User.repository;

import com.doch.User.model.DriverProfile;
import com.doch.User.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverProfileRepository extends JpaRepository<DriverProfile, String> {

    // Find a profile by the associated User object
    Optional<DriverProfile> findByUser(User user);

    // Find all profiles where the user is currently marked as available
    List<DriverProfile> findByUser_IsAvailableTrue();

    /**
     * Native PostgreSQL Query to find drivers within a radius (KM).
     * This is the foundation for the AI Auto-Assignment.
     */
    @Query(value = """
        SELECT dp.* FROM driver_profile dp
        INNER JOIN users u ON dp.user_id = u.id
        WHERE u.is_available = true
        AND (6371 * acos(cos(radians(:lat)) * cos(radians(cast(dp.last_latitude as double precision))) 
        * cos(radians(cast(dp.last_longitude as double precision)) - radians(:lng)) 
        + sin(radians(:lat)) * sin(radians(cast(dp.last_latitude as double precision))))) < :radius
        """, nativeQuery = true)
    List<DriverProfile> findNearbyActiveDrivers(@Param("lat") double lat, @Param("lng") double lng, @Param("radius") double radius);

}