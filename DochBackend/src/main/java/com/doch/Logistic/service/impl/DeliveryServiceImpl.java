package com.doch.Logistic.service.impl;

import com.doch.Catalog.service.StoreMediaUsecase;
import com.doch.Fleet.model.Vehicle;
import com.doch.Fleet.model.VehicleCategory;
import com.doch.Fleet.model.VehicleStatus;
import com.doch.Fleet.repository.VehicleRepository;
import com.doch.Fleet.service.VehicleDecisionMatrix;
import com.doch.Logistic.dto.*;
import com.doch.Logistic.model.*;
import com.doch.Logistic.repository.DeliveryEventRepository;
import com.doch.Logistic.repository.DeliveryRepository;
import com.doch.Logistic.repository.DochXenderDeliveryRequestRepository;
import com.doch.Logistic.service.DeliveryService;
import com.doch.Logistic.service.FulfilmentService;
import com.doch.Logistic.service.PricingService;
import com.doch.Logistic.service.TrackingEventService;
import com.doch.Notification.model.NotificationType;
import com.doch.Notification.service.NotificationService;
import com.doch.Order.model.Location;
import com.doch.Payment.dto.PaymentRequestDTO;
import com.doch.Payment.dto.PaymentResponseDTO;
import com.doch.Payment.model.PaymentStatus;
import com.doch.Payment.repository.PaymentRepository;
import com.doch.Payment.service.PaymentService;
import com.doch.User.model.DriverProfile;
import com.doch.User.model.Seller;
import com.doch.User.model.User;
import com.doch.User.repository.DriverProfileRepository;
import com.doch.User.repository.SellerRepository;
import com.doch.User.repository.UserRepository;
import com.doch.common.ImageUploadValidator;
import com.doch.common.ReferenceGenerator;
import com.doch.common.audit.AuditService;
import com.doch.common.exceptions.BusinessException;
import com.doch.common.exceptions.ResourceNotFoundException;
import com.doch.common.geocoding.DistanceService;
import com.doch.common.geocoding.GeocodingService;
import com.doch.common.settings.PricingSettings;
import com.doch.common.settings.PricingSettingsService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private static final Set<String> STAFF_VIEW = Set.of("VIEW_ALL_DELIVERIES", "ASSIGN_DRIVER", "MANAGE_SYSTEM");
    private static final Set<String> DISPATCH = Set.of("ASSIGN_DRIVER", "MANAGE_SYSTEM");

    private final DeliveryRepository deliveryRepository;
    private final DeliveryEventRepository eventRepository;
    private final DochXenderDeliveryRequestRepository xenderRequestRepository;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final VehicleRepository vehicleRepository;
    private final DeliveryMapper deliveryMapper;
    private final GeocodingService geocodingService;
    private final DistanceService distanceService;
    private final PricingService pricingService;
    private final PricingSettingsService settingsService;
    private final StoreMediaUsecase storeMediaUsecase;
    private final TrackingEventService tracking;
    private final FulfilmentService fulfilmentService;
    private final AuditService auditService;

    @Value("${application.delivery.max-otp-attempts:5}")
    private int maxOtpAttempts;

    /** Wrong OTP: the attempt counter must survive the rejection, so this exception does not roll back. */
    public static class OtpRejectedException extends BusinessException {
        public OtpRejectedException(String message) {
            super(message);
        }
    }

    // ================================================================== Xender creation & preview

    @Override
    @Transactional
    public DeliveryResponseDTO createDochXenderDelivery(DochXenderRequestDTO request, List<MultipartFile> images, User currentUser) {
        if (images == null || images.isEmpty()) {
            throw new BusinessException("Upload at least one photo of the sealed parcel before submitting the request.");
        }
        if (images.size() > 5) {
            throw new BusinessException("At most 5 parcel photos can be uploaded.");
        }
        images.forEach(img -> ImageUploadValidator.validate(img, "Parcel photo"));

        Location pickupLocation = geocodingService.geocodeAddress(request.getPickupLocationAddress())
                .orElseThrow(() -> new BusinessException("Could not determine coordinates for pickup address."));
        Location receiverLocation = geocodingService.geocodeAddress(request.getReceiverAddress())
                .orElseThrow(() -> new BusinessException("Could not determine coordinates for receiver address."));
        DistanceService.DistanceResult routeInfo = distanceService.getRoadDistance(
                pickupLocation.getLatitude(), pickupLocation.getLongitude(),
                receiverLocation.getLatitude(), receiverLocation.getLongitude()
        ).orElseThrow(() -> new BusinessException("Could not calculate road distance for pricing."));

        TransportType type = request.getTransportType() != null ? request.getTransportType() : TransportType.SINGLE_PARCEL;
        double price = pricingService.calculateDeliveryPrice(routeInfo.distanceKm(), type);
        PricingSettings settings = settingsService.getOrCreateSettings();
        VehicleCategory required = VehicleDecisionMatrix.requiredCategory(request.getPackageWeightKg(),
                request.getPackageMaxDimensionCm(), type, settings.getBicycleMaxWeightKg(),
                settings.getBicycleMaxDimensionCm(), settings.getKTruckMaxWeightKg());

        DochXenderDeliveryRequest xenderRequest = DochXenderDeliveryRequest.builder()
                .sender(currentUser)
                .pickupLocationAddress(request.getPickupLocationAddress().trim())
                .pickupLocation(pickupLocation)
                .receiverAddress(request.getReceiverAddress().trim())
                .receiverLocation(receiverLocation)
                .deliveryNote(request.getDeliveryNote())
                .price(price)
                .transportType(type)
                .status("PENDING_PAYMENT")
                .parcelImages(new ArrayList<>())
                .build();

        for (MultipartFile file : images) {
            try {
                String key = "xender/" + UUID.randomUUID() + ImageUploadValidator.extensionFor(file);
                String stored = storeMediaUsecase.storeInMinio(file.getBytes(), key, file.getContentType());
                xenderRequest.getParcelImages().add(DeliveryImage.builder().imageUrl(stored).contentType(file.getContentType()).build());
            } catch (Exception e) {
                log.error("Parcel image upload failed", e);
                throw new BusinessException("Failed to upload parcel images. Please try again.");
            }
        }
        DochXenderDeliveryRequest saved = xenderRequestRepository.save(xenderRequest);

        Delivery delivery = deliveryRepository.save(Delivery.builder()
                .deliveryRequest(saved)
                .trackingNumber(newTrackingNumber())
                .status(DeliveryStatus.PENDING_PAYMENT)
                .packageWeightKg(request.getPackageWeightKg())
                .packageMaxDimensionCm(request.getPackageMaxDimensionCm())
                .requiredVehicleCategory(required)
                .otpFailedAttempts(0)
                .estimatedDeliveryTime(LocalDateTime.now().plusMinutes((long) routeInfo.durationMinutes() + 60))
                .build());
        tracking.record(delivery, TrackingEventService.CREATED, null, DeliveryStatus.PENDING_PAYMENT,
                "Delivery request received - awaiting payment",
                String.format("%.1f km, %s, requires %s", routeInfo.distanceKm(), type, required), currentUser);
        auditService.record(currentUser, "XENDER_REQUEST_CREATED", "DELIVERY", delivery.getId(),
                String.format("%s, %.0f XAF, %s -> %s", delivery.getTrackingNumber(), price,
                        request.getPickupLocationAddress(), request.getReceiverAddress()));

        PaymentResponseDTO payment = paymentService.initiatePayment(PaymentRequestDTO.builder()
                .xenderRequestId(saved.getId())
                .paymentMethod(request.getPaymentMethod())
                .phone(request.getPaymentPhone())
                .build(), currentUser);

        notificationService.sendNotification(currentUser, NotificationType.DELIVERY_REQUEST_RECEIVED,
                String.format("Request %s received. Distance: %.1f km. Total: %.0f XAF.", delivery.getTrackingNumber(),
                        routeInfo.distanceKm(), price));

        DeliveryResponseDTO dto = deliveryMapper.toDto(delivery, DeliveryMapper.Perspective.OWNER, false);
        dto.setPaymentDetails(payment);
        return dto;
    }

    @Override
    public XenderPricePreviewResponse previewXenderPrice(XenderPricePreviewRequest request) {
        Location pickupLocation = geocodingService.geocodeAddress(request.getPickupLocationAddress())
                .orElseThrow(() -> new BusinessException("Could not determine coordinates for pickup address."));
        Location receiverLocation = geocodingService.geocodeAddress(request.getReceiverAddress())
                .orElseThrow(() -> new BusinessException("Could not determine coordinates for receiver address."));
        DistanceService.DistanceResult routeInfo = distanceService.getRoadDistance(
                pickupLocation.getLatitude(), pickupLocation.getLongitude(),
                receiverLocation.getLatitude(), receiverLocation.getLongitude()
        ).orElseThrow(() -> new BusinessException("Could not calculate road distance for pricing."));
        TransportType type = request.getTransportType() != null ? request.getTransportType() : TransportType.SINGLE_PARCEL;
        double price = pricingService.calculateDeliveryPrice(routeInfo.distanceKm(), type);
        return XenderPricePreviewResponse.builder()
                .price(price)
                .distanceKm(routeInfo.distanceKm())
                .estimatedDurationMinutes(routeInfo.durationMinutes())
                .transportType(type)
                .pickupLocationAddress(request.getPickupLocationAddress())
                .pickupLatitude(pickupLocation.getLatitude())
                .pickupLongitude(pickupLocation.getLongitude())
                .receiverAddress(request.getReceiverAddress())
                .receiverLatitude(receiverLocation.getLatitude())
                .receiverLongitude(receiverLocation.getLongitude())
                .priceBreakdown(String.format("%s, %.1f km: %.0f XAF", type == TransportType.FULL_TRUCK ? "Full truck" : "Single parcel",
                        routeInfo.distanceKm(), price))
                .build();
    }

    // ================================================================== seller

    @Override
    @Transactional
    public DeliveryResponseDTO markMarketDeliveryReady(String deliveryId, MultipartFile image, User seller) {
        ImageUploadValidator.validate(image, "Sealed package photo");
        Delivery delivery = lock(deliveryId);
        if (delivery.getMarketDeliveryRequest() == null || delivery.getMarketDeliveryRequest().getSeller() == null
                || !delivery.getMarketDeliveryRequest().getSeller().getUser().getId().equals(seller.getId())) {
            throw new AccessDeniedException("You are not the seller of this package.");
        }
        DeliveryStatus from = delivery.getStatus();
        if (from != DeliveryStatus.PENDING_PREPARATION) {
            throw new BusinessException("This package cannot be marked ready: it is currently " + from + ".");
        }
        try {
            String key = "sealed/" + delivery.getId() + "-" + UUID.randomUUID() + ImageUploadValidator.extensionFor(image);
            String stored = storeMediaUsecase.storeInMinio(image.getBytes(), key, image.getContentType());
            delivery.getMarketDeliveryRequest().setSealedProductImage(DeliveryImage.builder()
                    .imageUrl(stored).contentType(image.getContentType()).build());
        } catch (Exception e) {
            log.error("Sealed image upload failed", e);
            throw new BusinessException("Failed to upload the sealed package photo. Please try again.");
        }
        transition(delivery, DeliveryStatus.PENDING_ASSIGNMENT, TrackingEventService.STATUS_CHANGE,
                "Package sealed by the seller and ready for pickup", null, seller, null, null, null);

        String order = delivery.getMarketDeliveryRequest().getOrder().getReference();
        String dispatchMsg = String.format("Package %s (order %s) is ready for pickup at %s. Requires: %s.",
                delivery.getTrackingNumber(), order, delivery.getPickupAddress(), label(delivery.getRequiredVehicleCategory()));
        notificationService.sendNotificationToRole("ROLE_MARKET_ADMIN", NotificationType.ORDER_SHIPPED, dispatchMsg);
        notificationService.sendNotificationToRole("ROLE_XENDER_ADMIN", NotificationType.ORDER_SHIPPED, dispatchMsg);
        return deliveryMapper.toDto(delivery, DeliveryMapper.Perspective.SELLER, true);
    }

    // ================================================================== queries

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryResponseDTO> findDeliveriesForUser(User currentUser) {
        List<Delivery> all = new ArrayList<>(deliveryRepository.findByDeliveryRequest_Sender(currentUser));
        all.addAll(deliveryRepository.findByMarketDeliveryRequest_Order_Customer(currentUser));
        return all.stream()
                .sorted(Comparator.comparing(Delivery::getCreatedAt).reversed())
                .map(d -> deliveryMapper.toDto(d, DeliveryMapper.Perspective.OWNER, true))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryResponseDTO findDeliveryById(String deliveryIdOrTracking, User currentUser) {
        Delivery delivery = find(deliveryIdOrTracking);
        return deliveryMapper.toDto(delivery, perspectiveOf(delivery, currentUser), true);
    }

    private DeliveryMapper.Perspective perspectiveOf(Delivery delivery, User user) {
        User owner = delivery.getOwner();
        if (owner != null && owner.getId().equals(user.getId())) return DeliveryMapper.Perspective.OWNER;
        if (hasAny(user, STAFF_VIEW)) return DeliveryMapper.Perspective.STAFF;
        if (delivery.getDriver() != null && delivery.getDriver().getId().equals(user.getId())) return DeliveryMapper.Perspective.DRIVER;
        if (delivery.getMarketDeliveryRequest() != null && delivery.getMarketDeliveryRequest().getSeller() != null
                && delivery.getMarketDeliveryRequest().getSeller().getUser().getId().equals(user.getId())
                && delivery.getStatus() != DeliveryStatus.PENDING_PAYMENT) {
            return DeliveryMapper.Perspective.SELLER;
        }
        throw new AccessDeniedException("You are not authorized to view this delivery.");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DeliveryResponseDTO> searchDeliveries(String status, String query, String driverId, int page, int size, User viewer) {
        Specification<Delivery> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(status)) {
                List<DeliveryStatus> statuses = Arrays.stream(status.split(","))
                        .map(String::trim).filter(StringUtils::hasText).map(s -> parseStatus(s)).toList();
                predicates.add(root.get("status").in(statuses));
            }
            if (StringUtils.hasText(driverId)) {
                predicates.add(cb.equal(root.get("driver").get("id"), driverId));
            }
            if (StringUtils.hasText(query)) {
                String term = query.trim();
                Join<Object, Object> market = root.join("marketDeliveryRequest", JoinType.LEFT);
                Join<Object, Object> order = market.join("order", JoinType.LEFT);
                Join<Object, Object> seller = market.join("seller", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.equal(root.get("id"), term),
                        cb.equal(cb.upper(root.get("trackingNumber")), term.toUpperCase()),
                        cb.equal(cb.upper(order.get("reference")), term.toUpperCase()),
                        cb.equal(order.get("id"), term),
                        cb.like(cb.lower(seller.get("name")), "%" + term.toLowerCase() + "%")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return deliveryRepository.findAll(spec, pageable)
                .map(d -> deliveryMapper.toDto(d, DeliveryMapper.Perspective.STAFF, false));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> dispatchSummary() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (DeliveryStatus s : DeliveryStatus.values()) {
            counts.put(s.name(), deliveryRepository.countByStatus(s));
        }
        return counts;
    }

    // ================================================================== dispatch (staff)

    @Override
    @Transactional
    public DeliveryResponseDTO assignDriver(String deliveryId, DispatchAssignRequest request, User actor) {
        Delivery delivery = lock(deliveryId);
        DeliveryStatus from = delivery.getStatus();
        if (from == DeliveryStatus.PENDING_PREPARATION) {
            throw new BusinessException("Cannot assign a driver yet: the seller has not sealed the package.");
        }
        if (from != DeliveryStatus.PENDING_ASSIGNMENT && from != DeliveryStatus.AT_HUB && from != DeliveryStatus.DRIVER_ASSIGNED) {
            throw new BusinessException("A driver can only be assigned to packages that are ready, at a hub, or already assigned (reassignment). Current status: " + from + ".");
        }

        User driver = userRepository.findById(request.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found."));
        if (!driver.hasRole("ROLE_DELIVERY") || !driver.isEnabled()) {
            throw new BusinessException("The selected user is not an active driver.");
        }
        if (!Boolean.TRUE.equals(driver.getIsAvailable())) {
            throw new BusinessException("Driver " + driver.getFirstName() + " " + driver.getLastName() + " is offline. Ask them to go online in the driver app.");
        }
        DriverProfile profile = driverProfileRepository.findByUser(driver)
                .orElseThrow(() -> new BusinessException("The driver has no driver profile. Complete driver onboarding first."));

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found."));
        if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
            throw new BusinessException("Vehicle " + vehicle.getName() + " is " + vehicle.getStatus() + " and cannot be dispatched.");
        }
        if (vehicle.getCategory() == null) {
            throw new BusinessException("Vehicle " + vehicle.getName() + " has no fleet category (Bicycle / K-Truck / HiAce). Set it in Fleet management.");
        }
        if (!vehicle.getCategory().canCarry(delivery.getRequiredVehicleCategory())) {
            throw new BusinessException(String.format("Package %s requires at least a %s (%s kg); %s is a %s.",
                    delivery.getTrackingNumber(), label(delivery.getRequiredVehicleCategory()),
                    delivery.getPackageWeightKg() != null ? String.format("%.2f", delivery.getPackageWeightKg()) : "unknown weight",
                    vehicle.getName(), vehicle.getCategory().getLabel()));
        }
        bindVehicleToDriver(vehicle, profile, driver);

        User previousDriver = delivery.getDriver();
        boolean reassignment = from == DeliveryStatus.DRIVER_ASSIGNED;
        if (reassignment && previousDriver != null && previousDriver.getId().equals(driver.getId())
                && delivery.getVehicle() != null && delivery.getVehicle().getId().equals(vehicle.getId())) {
            throw new BusinessException("This driver and vehicle are already assigned to the package.");
        }

        delivery.setDriver(driver);
        delivery.setVehicle(vehicle);
        delivery.setAssignedAt(LocalDateTime.now());
        delivery.setScheduledFor(request.getScheduledFor());
        delivery.setFailureReason(null);
        String note = String.format("Driver %s %s, vehicle %s (%s, %s)%s%s", driver.getFirstName(), driver.getLastName(),
                vehicle.getName(), vehicle.getPlateNumber(), vehicle.getCategory().getLabel(),
                request.getScheduledFor() != null ? ", scheduled for " + request.getScheduledFor() : "",
                StringUtils.hasText(request.getNote()) ? ". " + request.getNote() : "");
        transition(delivery, DeliveryStatus.DRIVER_ASSIGNED, reassignment ? TrackingEventService.REASSIGNED : TrackingEventService.ASSIGNED,
                reassignment ? "A new driver has been assigned" : "A driver has been assigned", note, actor, null, null, null);
        auditService.record(actor, reassignment ? "DRIVER_REASSIGNED" : "DRIVER_ASSIGNED", "DELIVERY", delivery.getId(),
                delivery.getTrackingNumber() + ": " + note);

        notificationService.sendNotification(driver, NotificationType.DELIVERY_DRIVER_ASSIGNED,
                String.format("New job %s: pickup at %s, deliver to %s. Vehicle: %s (%s).", delivery.getTrackingNumber(),
                        delivery.getPickupAddress(), delivery.getDropoffAddress(), vehicle.getName(), vehicle.getPlateNumber()));
        if (reassignment && previousDriver != null && !previousDriver.getId().equals(driver.getId())) {
            notificationService.sendNotification(previousDriver, NotificationType.DELIVERY_DRIVER_ASSIGNED,
                    "Job " + delivery.getTrackingNumber() + " has been reassigned to another driver.");
        }
        User owner = delivery.getOwner();
        if (owner != null) {
            notificationService.sendNotification(owner, NotificationType.DELIVERY_DRIVER_ASSIGNED,
                    "A driver has been assigned to your package " + delivery.getTrackingNumber() + ".");
        }
        return deliveryMapper.toDto(delivery, DeliveryMapper.Perspective.STAFF, true);
    }

    /** A vehicle belongs to one driver at a time; switching vehicles is only allowed when the driver has no other active job on the old one. */
    private void bindVehicleToDriver(Vehicle vehicle, DriverProfile profile, User driver) {
        if (vehicle.getAssignedDriver() != null && !vehicle.getAssignedDriver().getId().equals(profile.getId())) {
            String holder = vehicle.getAssignedDriver().getUser() != null
                    ? vehicle.getAssignedDriver().getUser().getFirstName() + " " + vehicle.getAssignedDriver().getUser().getLastName()
                    : "another driver";
            throw new BusinessException("Vehicle " + vehicle.getName() + " is currently assigned to " + holder + ".");
        }
        vehicleRepository.findByAssignedDriverId(profile.getId()).ifPresent(current -> {
            if (current.getId().equals(vehicle.getId())) return;
            boolean busyOnCurrent = deliveryRepository.findByDriverAndStatusNotIn(driver,
                            List.of(DeliveryStatus.DELIVERED, DeliveryStatus.CANCELLED, DeliveryStatus.RETURNED)).stream()
                    .anyMatch(d -> d.getVehicle() != null && d.getVehicle().getId().equals(current.getId()));
            if (busyOnCurrent) {
                throw new BusinessException("The driver still has active jobs on vehicle " + current.getName()
                        + ". Finish or reassign them before switching vehicles.");
            }
            current.setAssignedDriver(null);
            vehicleRepository.saveAndFlush(current);
            auditService.record("VEHICLE_UNASSIGNED", "VEHICLE", current.getId(), "Driver moved to vehicle " + vehicle.getName());
        });
        if (vehicle.getAssignedDriver() == null) {
            vehicle.setAssignedDriver(profile);
            vehicleRepository.save(vehicle);
            auditService.record("VEHICLE_ASSIGNED", "VEHICLE", vehicle.getId(), "Assigned to driver " + driver.getEmail());
        }
    }

    @Override
    @Transactional
    public DeliveryResponseDTO unassignDriver(String deliveryId, DeliveryActionRequest request, User actor) {
        Delivery delivery = lock(deliveryId);
        if (delivery.getStatus() != DeliveryStatus.DRIVER_ASSIGNED) {
            throw new BusinessException("Only assigned packages that have not been collected can be unassigned.");
        }
        User previous = delivery.getDriver();
        String reason = reasonText(request, "Driver unavailable");
        delivery.setDriver(null);
        delivery.setVehicle(null);
        delivery.setAssignedAt(null);
        transition(delivery, DeliveryStatus.PENDING_ASSIGNMENT, TrackingEventService.UNASSIGNED,
                "Waiting for a new driver", reason, actor, null, null, null);
        auditService.record(actor, "DRIVER_UNASSIGNED", "DELIVERY", delivery.getId(), delivery.getTrackingNumber() + ": " + reason);
        if (previous != null) {
            notificationService.sendNotification(previous, NotificationType.DELIVERY_DRIVER_ASSIGNED,
                    "Job " + delivery.getTrackingNumber() + " was removed from your list.");
        }
        return deliveryMapper.toDto(delivery, DeliveryMapper.Perspective.STAFF, true);
    }

    @Override
    @Transactional
    public DeliveryResponseDTO hubCheckIn(String deliveryId, DeliveryActionRequest request, User actor) {
        Delivery delivery = lock(deliveryId);
        requireScanMatch(delivery, request.getTrackingCode());
        if (!StringUtils.hasText(request.getHubName())) {
            throw new BusinessException("Hub name is required for a hub scan.");
        }
        String hub = request.getHubName().trim();
        delivery.setCurrentHub(hub);
        delivery.setDriver(null);
        delivery.setVehicle(null);
        transition(delivery, DeliveryStatus.AT_HUB, TrackingEventService.HUB_SCAN,
                "Package scanned at hub " + hub, reasonText(request, "Hub check-in (scan, inspect, sort)"), actor, hub,
                request.getLatitude(), request.getLongitude());
        auditService.record(actor, "HUB_CHECK_IN", "DELIVERY", delivery.getId(), delivery.getTrackingNumber() + " at " + hub);
        return deliveryMapper.toDto(delivery, DeliveryMapper.Perspective.STAFF, true);
    }

    @Override
    @Transactional
    public DeliveryResponseDTO returnToSender(String deliveryId, DeliveryActionRequest request, User actor) {
        Delivery delivery = lock(deliveryId);
        String reason = reasonText(request, null);
        if (reason == null) throw new BusinessException("A reason is required to return a package.");
        delivery.setFailureReason(reason);
        transition(delivery, DeliveryStatus.RETURNED, TrackingEventService.STATUS_CHANGE,
                TrackingEventService.describe(DeliveryStatus.RETURNED), reason, actor, null, null, null);
        auditService.record(actor, "PACKAGE_RETURNED", "DELIVERY", delivery.getId(), delivery.getTrackingNumber() + ": " + reason);
        notifyOwner(delivery, NotificationType.DELIVERY_FAILED,
                "Package " + delivery.getTrackingNumber() + " is being returned to the sender: " + reason);
        flagPackageRefund(delivery, actor, "Package returned: " + reason);
        return deliveryMapper.toDto(delivery, DeliveryMapper.Perspective.STAFF, true);
    }

    @Override
    @Transactional
    public DeliveryResponseDTO cancelDelivery(String deliveryId, DeliveryActionRequest request, User actor) {
        Delivery delivery = lock(deliveryId);
        String reason = reasonText(request, null);
        if (reason == null) throw new BusinessException("A reason is required to cancel a package.");
        boolean wasPaid = delivery.getStatus() != DeliveryStatus.PENDING_PAYMENT;
        delivery.setFailureReason(reason);
        transition(delivery, DeliveryStatus.CANCELLED, TrackingEventService.STATUS_CHANGE,
                TrackingEventService.describe(DeliveryStatus.CANCELLED), reason, actor, null, null, null);
        if (delivery.getDeliveryRequest() != null) {
            delivery.getDeliveryRequest().setStatus("CANCELLED");
            xenderRequestRepository.save(delivery.getDeliveryRequest());
        }
        auditService.record(actor, "PACKAGE_CANCELLED", "DELIVERY", delivery.getId(), delivery.getTrackingNumber() + ": " + reason);
        notifyOwner(delivery, NotificationType.DELIVERY_FAILED, "Package " + delivery.getTrackingNumber() + " was cancelled: " + reason);
        if (wasPaid) flagPackageRefund(delivery, actor, "Package cancelled: " + reason);
        return deliveryMapper.toDto(delivery, DeliveryMapper.Perspective.STAFF, true);
    }

    @Override
    @Transactional
    public DeliveryResponseDTO resetOtp(String deliveryId, DeliveryActionRequest request, User actor) {
        Delivery delivery = lock(deliveryId);
        if (DeliveryStateMachine.isTerminal(delivery.getStatus())) {
            throw new BusinessException("This package is already closed.");
        }
        delivery.setOtpFailedAttempts(0);
        delivery.setDeliveryOtp(ReferenceGenerator.numericOtp(6));
        deliveryRepository.save(delivery);
        tracking.record(delivery, TrackingEventService.OTP_RESET, delivery.getStatus(), delivery.getStatus(), null,
                reasonText(request, "Delivery code reset by dispatch after identity check"), actor);
        auditService.record(actor, "OTP_RESET", "DELIVERY", delivery.getId(), delivery.getTrackingNumber());
        notifyOwner(delivery, NotificationType.DELIVERY_IN_TRANSIT,
                "A new delivery code was issued for package " + delivery.getTrackingNumber() + ". Find it in your order details.");
        return deliveryMapper.toDto(delivery, DeliveryMapper.Perspective.STAFF, true);
    }

    // ================================================================== driver actions

    @Override
    @Transactional
    public DeliveryResponseDTO confirmPickup(String deliveryId, DeliveryActionRequest request, User driver) {
        Delivery delivery = lockForDriver(deliveryId, driver);
        if (delivery.getStatus() != DeliveryStatus.DRIVER_ASSIGNED) {
            throw new BusinessException("Pickup can only be confirmed for an assigned package (current status: " + delivery.getStatus() + ").");
        }
        requireScanMatch(delivery, request != null ? request.getTrackingCode() : null);
        delivery.setStartTime(LocalDateTime.now());
        delivery.setCurrentHub(null);
        transition(delivery, DeliveryStatus.PICKED_UP, TrackingEventService.PICKUP_SCAN,
                TrackingEventService.describe(DeliveryStatus.PICKED_UP),
                "Scanned code " + request.getTrackingCode().trim().toUpperCase(), driver, null,
                request.getLatitude(), request.getLongitude());
        notifyOwner(delivery, NotificationType.DELIVERY_PICKED_UP,
                "Your package " + delivery.getTrackingNumber() + " has been collected by the driver.");
        return deliveryMapper.toDto(delivery, DeliveryMapper.Perspective.DRIVER, true);
    }

    @Override
    @Transactional
    public DeliveryResponseDTO markInTransit(String deliveryId, DeliveryActionRequest request, User driver) {
        Delivery delivery = lockForDriver(deliveryId, driver);
        DeliveryStatus from = delivery.getStatus();
        if (from != DeliveryStatus.PICKED_UP && from != DeliveryStatus.FAILED) {
            throw new BusinessException("Only collected packages can go out for delivery (current status: " + from + ").");
        }
        delivery.setFailureReason(null);
        transition(delivery, DeliveryStatus.IN_TRANSIT, TrackingEventService.STATUS_CHANGE,
                from == DeliveryStatus.FAILED ? "Delivery re-attempt in progress" : TrackingEventService.describe(DeliveryStatus.IN_TRANSIT),
                request != null ? request.getNote() : null, driver, null,
                request != null ? request.getLatitude() : null, request != null ? request.getLongitude() : null);
        notifyOwner(delivery, NotificationType.DELIVERY_IN_TRANSIT,
                "Your package " + delivery.getTrackingNumber() + " is out for delivery. Have your delivery code ready.");
        return deliveryMapper.toDto(delivery, DeliveryMapper.Perspective.DRIVER, true);
    }

    @Override
    @Transactional
    public DeliveryResponseDTO reportFailure(String deliveryId, DeliveryActionRequest request, User driver) {
        Delivery delivery = lockForDriver(deliveryId, driver);
        String reason = reasonText(request, null);
        if (reason == null) {
            throw new BusinessException("Select a reason (e.g. recipient unavailable, damaged package, details mismatch).");
        }
        delivery.setFailureReason(reason);
        transition(delivery, DeliveryStatus.FAILED, TrackingEventService.EXCEPTION,
                TrackingEventService.describe(DeliveryStatus.FAILED), reason, driver, null,
                request.getLatitude(), request.getLongitude());
        notifyOwner(delivery, NotificationType.DELIVERY_FAILED,
                "Delivery attempt for " + delivery.getTrackingNumber() + " was unsuccessful (" + humanReason(request.getReason())
                        + "). We will contact you to reschedule.");
        String adminMsg = String.format("DELIVERY EXCEPTION %s by %s %s: %s", delivery.getTrackingNumber(),
                driver.getFirstName(), driver.getLastName(), reason);
        notificationService.sendNotificationToRole("ROLE_XENDER_ADMIN", NotificationType.DELIVERY_FAILED, adminMsg);
        notificationService.sendNotificationToRole("ROLE_MARKET_ADMIN", NotificationType.DELIVERY_FAILED, adminMsg);
        auditService.record(driver, "DELIVERY_FAILED", "DELIVERY", delivery.getId(), adminMsg);
        return deliveryMapper.toDto(delivery, DeliveryMapper.Perspective.DRIVER, true);
    }

    @Override
    @Transactional
    public DeliveryResponseDTO reportNotCollected(String deliveryId, DeliveryActionRequest request, User driver) {
        Delivery delivery = lockForDriver(deliveryId, driver);
        if (delivery.getStatus() != DeliveryStatus.DRIVER_ASSIGNED) {
            throw new BusinessException("Only packages awaiting pickup can be reported as not collected.");
        }
        String reason = reasonText(request, null);
        if (reason == null) throw new BusinessException("A reason is required.");
        delivery.setDriver(null);
        delivery.setVehicle(null);
        delivery.setAssignedAt(null);
        delivery.setFailureReason(reason);
        transition(delivery, DeliveryStatus.PENDING_ASSIGNMENT, TrackingEventService.EXCEPTION,
                "Pickup could not be completed - a new pickup will be scheduled", reason, driver, null,
                request.getLatitude(), request.getLongitude());
        String msg = String.format("PICKUP FAILED %s (%s): %s", delivery.getTrackingNumber(), delivery.getPickupAddress(), reason);
        notificationService.sendNotificationToRole("ROLE_XENDER_ADMIN", NotificationType.DELIVERY_FAILED, msg);
        notificationService.sendNotificationToRole("ROLE_MARKET_ADMIN", NotificationType.DELIVERY_FAILED, msg);
        if (delivery.getMarketDeliveryRequest() != null && delivery.getMarketDeliveryRequest().getSeller() != null) {
            notificationService.sendNotification(delivery.getMarketDeliveryRequest().getSeller().getUser(), NotificationType.DELIVERY_FAILED,
                    "The driver could not collect package " + delivery.getTrackingNumber() + ": " + reason);
        }
        auditService.record(driver, "PICKUP_FAILED", "DELIVERY", delivery.getId(), msg);
        return deliveryMapper.toDto(delivery, DeliveryMapper.Perspective.DRIVER, false);
    }

    @Override
    @Transactional(noRollbackFor = OtpRejectedException.class)
    public DeliveryResponseDTO verifyAndCompleteDelivery(String deliveryId, String otp, MultipartFile podImage,
                                                         String latitude, String longitude, User driver) {
        Delivery delivery = lockForDriver(deliveryId, driver);
        DeliveryStatus from = delivery.getStatus();
        if (from != DeliveryStatus.IN_TRANSIT && from != DeliveryStatus.PICKED_UP) {
            throw new BusinessException("This package cannot be completed in status " + from + ".");
        }
        if (deliveryMapper.isOtpLocked(delivery)) {
            throw new BusinessException("Too many wrong codes. The delivery is locked - contact dispatch to verify the recipient.");
        }
        if (!StringUtils.hasText(otp) || delivery.getDeliveryOtp() == null
                || !MessageDigest.isEqual(delivery.getDeliveryOtp().getBytes(StandardCharsets.UTF_8),
                otp.trim().getBytes(StandardCharsets.UTF_8))) {
            int attempts = (delivery.getOtpFailedAttempts() == null ? 0 : delivery.getOtpFailedAttempts()) + 1;
            delivery.setOtpFailedAttempts(attempts);
            deliveryRepository.save(delivery);
            boolean locked = attempts >= maxOtpAttempts;
            tracking.record(delivery, locked ? TrackingEventService.OTP_LOCKED : TrackingEventService.OTP_FAILED, from, from,
                    null, "Wrong delivery code entered (attempt " + attempts + "/" + maxOtpAttempts + ")", driver, null, latitude, longitude);
            if (locked) {
                auditService.record(driver, "OTP_LOCKED", "DELIVERY", delivery.getId(), delivery.getTrackingNumber());
                notificationService.sendNotificationToRole("ROLE_XENDER_ADMIN", NotificationType.DELIVERY_FAILED,
                        "Delivery code locked for " + delivery.getTrackingNumber() + " after " + attempts + " wrong attempts.");
                throw new OtpRejectedException("Invalid code. The delivery is now locked - contact dispatch.");
            }
            throw new OtpRejectedException("Invalid code. Delivery cannot be completed. " + (maxOtpAttempts - attempts) + " attempt(s) left.");
        }
        ImageUploadValidator.validate(podImage, "Proof of delivery photo");

        try {
            String key = "pod/" + delivery.getId() + "-" + UUID.randomUUID() + ImageUploadValidator.extensionFor(podImage);
            String stored = storeMediaUsecase.storeInMinio(podImage.getBytes(), key, podImage.getContentType());
            delivery.setProofOfDeliveryImage(DeliveryImage.builder().imageUrl(stored).contentType(podImage.getContentType()).build());
        } catch (Exception e) {
            log.error("POD upload failed", e);
            throw new BusinessException("Failed to save the proof of delivery photo. Please try again.");
        }

        if (from == DeliveryStatus.PICKED_UP) {
            transition(delivery, DeliveryStatus.IN_TRANSIT, TrackingEventService.STATUS_CHANGE,
                    TrackingEventService.describe(DeliveryStatus.IN_TRANSIT), null, driver, null, latitude, longitude);
        }
        delivery.setEndTime(LocalDateTime.now());
        String vehicleText = delivery.getVehicle() != null ? delivery.getVehicle().getName() + " (" + delivery.getVehicle().getPlateNumber() + ")" : "n/a";
        transition(delivery, DeliveryStatus.DELIVERED, TrackingEventService.DELIVERED,
                TrackingEventService.describe(DeliveryStatus.DELIVERED),
                "OTP verified, proof of delivery photo stored. Driver " + driver.getFirstName() + " " + driver.getLastName()
                        + ", vehicle " + vehicleText, driver, null, latitude, longitude);

        driverProfileRepository.findByUser(driver).ifPresent(p -> {
            p.setTotalDeliveries((p.getTotalDeliveries() == null ? 0 : p.getTotalDeliveries()) + 1);
            driverProfileRepository.save(p);
        });
        auditService.record(driver, "PACKAGE_DELIVERED", "DELIVERY", delivery.getId(), delivery.getTrackingNumber());
        notifyOwner(delivery, NotificationType.DELIVERY_COMPLETED,
                "Package " + delivery.getTrackingNumber() + " was delivered successfully.");
        return deliveryMapper.toDto(delivery, DeliveryMapper.Perspective.DRIVER, true);
    }

    // ================================================================== public tracking

    @Override
    @Transactional(readOnly = true)
    public PublicTrackingDTO publicTracking(String trackingNumber) {
        Delivery delivery = deliveryRepository.findByTrackingNumberIgnoreCase(trackingNumber == null ? "" : trackingNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("No package found with tracking number " + trackingNumber));
        List<DeliveryEvent> events = eventRepository.findByDeliveryOrderByOccurredAtAsc(delivery);
        List<DeliveryEventDTO> publicEvents = events.stream()
                .filter(e -> e.getPublicMessage() != null)
                .map(e -> DeliveryEventDTO.builder()
                        .eventType(e.getEventType())
                        .toStatus(e.getToStatus() != null ? e.getToStatus().name() : null)
                        .message(e.getPublicMessage())
                        .hubName(e.getHubName())
                        .occurredAt(e.getOccurredAt())
                        .build())
                .toList();
        return PublicTrackingDTO.builder()
                .trackingNumber(delivery.getTrackingNumber())
                .status(delivery.getStatus().name())
                .vehicleCategory(delivery.getVehicle() != null && delivery.getVehicle().getCategory() != null
                        ? delivery.getVehicle().getCategory().getLabel() : null)
                .estimatedDeliveryTime(delivery.getEstimatedDeliveryTime())
                .lastUpdated(events.isEmpty() ? null : events.get(events.size() - 1).getOccurredAt())
                .events(publicEvents)
                .build();
    }

    // ================================================================== helpers

    /** Applies a validated status change, writes the tracking event and propagates the change to the order. */
    private void transition(Delivery delivery, DeliveryStatus to, String eventType, String publicMessage, String internalNote,
                            User actor, String hub, String lat, String lng) {
        DeliveryStatus from = delivery.getStatus();
        DeliveryStateMachine.assertTransition(from, to);
        delivery.setStatus(to);
        deliveryRepository.save(delivery);
        tracking.record(delivery, eventType, from, to, publicMessage, internalNote, actor, hub, lat, lng);
        fulfilmentService.afterDeliveryStatusChange(delivery, from, actor);
    }

    private Delivery find(String idOrTracking) {
        if (!StringUtils.hasText(idOrTracking)) throw new ResourceNotFoundException("Delivery not found");
        String key = idOrTracking.trim();
        return deliveryRepository.findById(key)
                .or(() -> deliveryRepository.findByTrackingNumberIgnoreCase(key))
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found: " + key));
    }

    private Delivery lock(String idOrTracking) {
        return deliveryRepository.findByIdForUpdate(find(idOrTracking).getId())
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found"));
    }

    private Delivery lockForDriver(String deliveryId, User driver) {
        Delivery delivery = lock(deliveryId);
        if (delivery.getDriver() == null || !delivery.getDriver().getId().equals(driver.getId())) {
            throw new AccessDeniedException("This package is not assigned to you.");
        }
        return delivery;
    }

    private void requireScanMatch(Delivery delivery, String scannedCode) {
        if (!StringUtils.hasText(scannedCode)) {
            throw new BusinessException("Scan the package QR code or type its tracking number to confirm.");
        }
        String code = scannedCode.trim();
        // QR labels may encode a tracking URL; accept it when it ends with the tracking number.
        if (!code.equalsIgnoreCase(delivery.getTrackingNumber())
                && !code.toUpperCase().endsWith("/" + delivery.getTrackingNumber().toUpperCase())) {
            throw new BusinessException("Scanned code " + code + " does not match package " + delivery.getTrackingNumber() + ".");
        }
    }

    private void notifyOwner(Delivery delivery, NotificationType type, String message) {
        User owner = delivery.getOwner();
        if (owner != null) notificationService.sendNotification(owner, type, message);
    }

    private void flagPackageRefund(Delivery delivery, User actor, String why) {
        boolean paid;
        if (delivery.getDeliveryRequest() != null) {
            paid = paymentRepository.findByXenderRequest(delivery.getDeliveryRequest())
                    .map(p -> p.getStatus() == PaymentStatus.COMPLETED).orElse(false);
        } else {
            paid = delivery.getMarketDeliveryRequest().getOrder().getPaidAt() != null;
        }
        if (!paid) return;
        String msg = String.format("REFUND REVIEW REQUIRED for package %s: %s", delivery.getTrackingNumber(), why);
        auditService.record(actor, "PAYMENT_REFUND_REQUIRED", "DELIVERY", delivery.getId(), msg);
        notificationService.sendNotificationToRole("ROLE_SUPER_ADMIN", NotificationType.REFUND_PROCESSED, msg);
    }

    private String newTrackingNumber() {
        for (int i = 0; i < 10; i++) {
            String ref = ReferenceGenerator.trackingNumber();
            if (!deliveryRepository.existsByTrackingNumber(ref)) return ref;
        }
        throw new IllegalStateException("Could not generate a unique tracking number");
    }

    private static String reasonText(DeliveryActionRequest request, String fallback) {
        if (request == null) return fallback;
        String code = StringUtils.hasText(request.getReason()) ? humanReason(request.getReason()) : null;
        String note = StringUtils.hasText(request.getNote()) ? request.getNote().trim() : null;
        if (code == null && note == null) return fallback;
        if (code == null) return note;
        return note == null ? code : code + " - " + note;
    }

    private static String humanReason(String code) {
        if (code == null) return "unspecified";
        return switch (code.trim().toUpperCase()) {
            case "RECIPIENT_UNAVAILABLE" -> "Recipient unavailable";
            case "ADDRESS_NOT_FOUND" -> "Address not found";
            case "DAMAGED" -> "Package damaged";
            case "DETAILS_MISMATCH" -> "Package details do not match";
            case "REFUSED" -> "Recipient refused the package";
            case "PACKAGE_NOT_READY" -> "Package not ready at pickup";
            case "SENDER_UNAVAILABLE" -> "Sender / seller unavailable";
            case "DRIVER_UNAVAILABLE" -> "Driver unavailable";
            case "VEHICLE_ISSUE" -> "Vehicle issue";
            default -> code.trim();
        };
    }

    private static String label(VehicleCategory category) {
        return category != null ? category.getLabel() : "any vehicle";
    }

    private static DeliveryStatus parseStatus(String raw) {
        try {
            return DeliveryStatus.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid delivery status: " + raw);
        }
    }

    private static boolean hasAny(User user, Set<String> authorities) {
        return user.getAuthorities().stream().anyMatch(a -> authorities.contains(a.getAuthority()));
    }
}
