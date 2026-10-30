package com.doch.Logistic.service;

import com.doch.Logistic.dto.*;
import com.doch.User.model.User;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface DeliveryService {

    XenderPricePreviewResponse previewXenderPrice(XenderPricePreviewRequest request);

    DeliveryResponseDTO createDochXenderDelivery(DochXenderRequestDTO request, List<MultipartFile> images, User currentUser);

    /** Seller confirms the package is sealed (photo) - moves it to the dispatch queue. */
    DeliveryResponseDTO markMarketDeliveryReady(String deliveryId, MultipartFile image, User seller);

    List<DeliveryResponseDTO> findDeliveriesForUser(User currentUser);

    /** Accepts the delivery ID or its tracking number. */
    DeliveryResponseDTO findDeliveryById(String deliveryIdOrTracking, User currentUser);

    Page<DeliveryResponseDTO> searchDeliveries(String status, String query, String driverId, int page, int size, User viewer);

    Map<String, Long> dispatchSummary();

    // --- dispatch ---
    DeliveryResponseDTO assignDriver(String deliveryId, DispatchAssignRequest request, User actor);

    DeliveryResponseDTO unassignDriver(String deliveryId, DeliveryActionRequest request, User actor);

    DeliveryResponseDTO hubCheckIn(String deliveryId, DeliveryActionRequest request, User actor);

    DeliveryResponseDTO returnToSender(String deliveryId, DeliveryActionRequest request, User actor);

    DeliveryResponseDTO cancelDelivery(String deliveryId, DeliveryActionRequest request, User actor);

    DeliveryResponseDTO resetOtp(String deliveryId, DeliveryActionRequest request, User actor);

    // --- driver ---
    DeliveryResponseDTO confirmPickup(String deliveryId, DeliveryActionRequest request, User driver);

    DeliveryResponseDTO markInTransit(String deliveryId, DeliveryActionRequest request, User driver);

    DeliveryResponseDTO reportFailure(String deliveryId, DeliveryActionRequest request, User driver);

    DeliveryResponseDTO reportNotCollected(String deliveryId, DeliveryActionRequest request, User driver);

    DeliveryResponseDTO verifyAndCompleteDelivery(String deliveryId, String otp, MultipartFile podImage,
                                                  String latitude, String longitude, User driver);

    // --- tracking ---
    PublicTrackingDTO publicTracking(String trackingNumber);
}
