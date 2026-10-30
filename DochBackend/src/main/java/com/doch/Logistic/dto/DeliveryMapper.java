package com.doch.Logistic.dto;

import com.doch.Catalog.service.StoreMediaUsecase;
import com.doch.Logistic.model.*;
import com.doch.Logistic.repository.DeliveryEventRepository;
import com.doch.Order.model.Location;
import com.doch.Order.model.Order;
import com.doch.Payment.dto.PaymentMapper;
import com.doch.Payment.model.Payment;
import com.doch.Payment.repository.PaymentRepository;
import com.doch.User.model.Seller;
import com.doch.User.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryMapper {

    /** Who is looking at the package: drives which fields are exposed. */
    public enum Perspective { OWNER, DRIVER, STAFF, SELLER }

    private final PaymentMapper paymentMapper;
    private final PaymentRepository paymentRepository;
    private final StoreMediaUsecase storeMediaUsecase;
    private final DeliveryEventRepository eventRepository;

    @Value("${application.delivery.max-otp-attempts:5}")
    private int maxOtpAttempts;

    public DeliveryResponseDTO toDto(Delivery delivery, Perspective perspective, boolean includeEvents) {
        if (delivery == null) return null;

        DeliveryResponseDTO.DeliveryResponseDTOBuilder builder = DeliveryResponseDTO.builder()
                .deliveryId(delivery.getId())
                .trackingNumber(delivery.getTrackingNumber())
                .status(delivery.getStatus() != null ? delivery.getStatus().name() : "UNKNOWN")
                .createdAt(toLocal(delivery.getCreatedAt()))
                .assignedAt(delivery.getAssignedAt())
                .pickedUpAt(delivery.getStartTime())
                .deliveredAt(delivery.getEndTime())
                .estimatedDeliveryTime(delivery.getEstimatedDeliveryTime())
                .scheduledFor(delivery.getScheduledFor())
                .failureReason(delivery.getFailureReason())
                .currentHub(delivery.getCurrentHub())
                .packageWeightKg(delivery.getPackageWeightKg())
                .packageMaxDimensionCm(delivery.getPackageMaxDimensionCm())
                .requiredVehicleCategory(delivery.getRequiredVehicleCategory() != null
                        ? delivery.getRequiredVehicleCategory().name() : null)
                .otpFailedAttempts(delivery.getOtpFailedAttempts())
                .otpLocked(isOtpLocked(delivery))
                .pickupLocationAddress(delivery.getPickupAddress())
                .receiverAddress(delivery.getDropoffAddress());

        User driver = delivery.getDriver();
        if (driver != null) {
            builder.driverId(driver.getId())
                    .driverName(driver.getFirstName() + " " + driver.getLastName());
            if (perspective != Perspective.SELLER) {
                builder.driverPhone(driver.getTel());
            }
        }
        if (delivery.getVehicle() != null) {
            builder.vehicle(DeliveryResponseDTO.VehicleSummary.builder()
                    .id(delivery.getVehicle().getId())
                    .name(delivery.getVehicle().getName())
                    .plateNumber(delivery.getVehicle().getPlateNumber())
                    .category(delivery.getVehicle().getCategory() != null ? delivery.getVehicle().getCategory().name() : null)
                    .vehicleType(delivery.getVehicle().getVehicleType())
                    .build());
        }

        if (perspective == Perspective.OWNER) {
            builder.deliveryOtp(delivery.getDeliveryOtp());
        }

        if (delivery.getDeliveryRequest() != null) {
            DochXenderDeliveryRequest xender = delivery.getDeliveryRequest();
            User sender = xender.getSender();
            builder.source("XENDER")
                    .receiverName(sender != null ? sender.getFirstName() + " " + sender.getLastName() : null)
                    .receiverPhone(sender != null ? sender.getTel() : null)
                    .deliveryNote(xender.getDeliveryNote())
                    .price(xender.getPrice())
                    .transportType(xender.getTransportType())
                    .pickupLocationCoords(toLocationDto(xender.getPickupLocation()))
                    .receiverLocationCoords(toLocationDto(xender.getReceiverLocation()));
            if (xender.getParcelImages() != null) {
                builder.parcelImageUrls(xender.getParcelImages().stream()
                        .map(img -> safeUrl(img.getImageUrl())).filter(u -> u != null).toList());
            }
            if (perspective == Perspective.OWNER || perspective == Perspective.STAFF) {
                Payment payment = paymentRepository.findByXenderRequest(xender).orElse(null);
                builder.paymentDetails(paymentMapper.toDto(payment));
            }
        } else if (delivery.getMarketDeliveryRequest() != null) {
            DochMarketDeliveryRequest marketReq = delivery.getMarketDeliveryRequest();
            Order order = marketReq.getOrder();
            User customer = order.getCustomer();
            Seller seller = marketReq.getSeller();
            builder.source("MARKET")
                    .orderId(order.getId())
                    .orderReference(order.getReference())
                    .sellerName(seller != null ? seller.getName() : null)
                    .sellerPhone(seller != null && perspective != Perspective.OWNER ? seller.getContactNumber() : null)
                    .receiverName(customer.getFirstName() + " " + customer.getLastName())
                    .deliveryNote(marketReq.getDeliveryNote())
                    .transportType(order.getTransportType())
                    .pickupLocationCoords(toLocationDto(marketReq.getPickupLocation()))
                    .receiverLocationCoords(toLocationDto(marketReq.getReceiverLocation()));
            if (perspective != Perspective.SELLER) {
                builder.receiverPhone(order.getContactPhone() != null ? order.getContactPhone() : customer.getTel());
                builder.price(marketReq.getPrice());
            }
            List<String> imagesForDriver = new ArrayList<>();
            if (marketReq.getSealedProductImage() != null) {
                String url = safeUrl(marketReq.getSealedProductImage().getImageUrl());
                if (url != null) imagesForDriver.add(url);
            }
            builder.parcelImageUrls(imagesForDriver);
            if (perspective == Perspective.OWNER || perspective == Perspective.STAFF) {
                Payment payment = paymentRepository.findByOrder(order).orElse(null);
                builder.paymentDetails(paymentMapper.toDto(payment));
            }
        }

        if (delivery.getProofOfDeliveryImage() != null && perspective != Perspective.SELLER) {
            builder.proofOfDeliveryUrl(safeUrl(delivery.getProofOfDeliveryImage().getImageUrl()));
        }

        if (includeEvents) {
            builder.events(toEventDtos(eventRepository.findByDeliveryOrderByOccurredAtAsc(delivery),
                    perspective == Perspective.STAFF || perspective == Perspective.DRIVER));
        }
        return builder.build();
    }

    public List<DeliveryEventDTO> toEventDtos(List<DeliveryEvent> events, boolean staffView) {
        return events.stream().map(e -> DeliveryEventDTO.builder()
                .eventType(e.getEventType())
                .fromStatus(e.getFromStatus() != null ? e.getFromStatus().name() : null)
                .toStatus(e.getToStatus() != null ? e.getToStatus().name() : null)
                .message(e.getPublicMessage())
                .internalNote(staffView ? e.getInternalNote() : null)
                .actorName(staffView ? e.getActorName() : null)
                .actorRole(e.getActorRole())
                .hubName(e.getHubName())
                .occurredAt(e.getOccurredAt())
                .build()).toList();
    }

    public boolean isOtpLocked(Delivery delivery) {
        return delivery.getOtpFailedAttempts() != null && delivery.getOtpFailedAttempts() >= maxOtpAttempts;
    }

    private String safeUrl(String key) {
        if (key == null) return null;
        try {
            return storeMediaUsecase.getPresignedUrl(key);
        } catch (Exception e) {
            log.warn("Could not create presigned URL for {}: {}", key, e.getMessage());
            return null;
        }
    }

    private static LocalDateTime toLocal(Date date) {
        return date != null ? date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null;
    }

    private LocationDTO toLocationDto(Location location) {
        if (location == null) return null;
        return LocationDTO.builder()
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .build();
    }
}