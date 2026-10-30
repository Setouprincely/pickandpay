package com.doch.common.geocoding;

import com.doch.Logistic.dto.RouteStopDTO;
import com.doch.User.model.User;
import java.util.List;

public interface NavigationService {
    /**
     * Calculates the best sequence of pickups and drop-offs
     * to minimize distance while ensuring Pickups happen before Drop-offs.
     */
    List<RouteStopDTO> getOptimizedRoute(User driver);
}