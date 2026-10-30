package com.doch.Logistic.controller;

import com.doch.Logistic.dto.*;
import com.doch.Logistic.service.DeliveryService;
import com.doch.User.model.User;
import com.doch.common.exceptions.BusinessException;
import com.doch.common.geocoding.NavigationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
@Tag(name = "3. Logistics & Delivery", description = "Package lifecycle: Xender requests, seller preparation, dispatch, pickup scan, hub processing, OTP delivery and exceptions.")
@SecurityRequirement(name = "bearerAuth")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final NavigationService navigationService;

    private static User user(Authentication auth) {
        return (User) auth.getPrincipal();
    }

    // --- CUSTOMER / SENDER ---

    @Operation(summary = "Preview Xender delivery cost")
    @PostMapping("/preview")
    @PreAuthorize("hasAuthority('PLACE_ORDER')")
    public ResponseEntity<XenderPricePreviewResponse> previewXenderPrice(@Valid @RequestBody XenderPricePreviewRequest request) {
        return ResponseEntity.ok(deliveryService.previewXenderPrice(request));
    }

    @Operation(summary = "Create a new Xender request (parcel photos mandatory)",
            description = "Creates the package (with tracking number) and starts its payment. Pass paymentPhone for a direct mobile money prompt.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PLACE_ORDER')")
    public ResponseEntity<DeliveryResponseDTO> createDochXenderDelivery(
            @RequestPart("request") @Valid DochXenderRequestDTO request,
            @RequestPart("images") List<MultipartFile> images,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deliveryService.createDochXenderDelivery(request, images, user(authentication)));
    }

    @Operation(summary = "User: My packages", description = "Xender requests and marketplace packages of the logged-in user, with tracking history and OTP.")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DeliveryResponseDTO>> getMyDeliveries(Authentication authentication) {
        return ResponseEntity.ok(deliveryService.findDeliveriesForUser(user(authentication)));
    }

    @Operation(summary = "Get package details by ID or tracking number",
            description = "Owner, assigned driver, involved seller or logistics staff. The OTP is only returned to the owner.")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DeliveryResponseDTO> getDeliveryById(@PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(deliveryService.findDeliveryById(id, user(auth)));
    }

    // --- SELLER ---

    @Operation(summary = "Seller: Mark package ready (upload sealed photo)",
            description = "Moves a paid marketplace package from PENDING_PREPARATION to PENDING_ASSIGNMENT (dispatch queue).")
    @PostMapping(value = "/market/{id}/ready", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_SELLER')")
    public ResponseEntity<DeliveryResponseDTO> markReady(@PathVariable String id,
                                                         @RequestPart("image") MultipartFile image,
                                                         Authentication authentication) {
        return ResponseEntity.ok(deliveryService.markMarketDeliveryReady(id, image, user(authentication)));
    }

    // --- STAFF / DISPATCH ---

    @Operation(summary = "Staff: Dispatch queue / search packages (paginated)",
            description = "status: one or more comma-separated statuses (e.g. PENDING_ASSIGNMENT,AT_HUB). q: tracking number, delivery ID, order reference/ID or seller name.")
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('VIEW_ALL_DELIVERIES')")
    public ResponseEntity<Page<DeliveryResponseDTO>> searchDeliveries(@RequestParam(required = false) String status,
                                                                      @RequestParam(required = false) String q,
                                                                      @RequestParam(required = false) String driverId,
                                                                      @RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "20") int size,
                                                                      Authentication auth) {
        return ResponseEntity.ok(deliveryService.searchDeliveries(status, q, driverId, page, size, user(auth)));
    }

    @Operation(summary = "Staff: Package counts per status")
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('VIEW_ALL_DELIVERIES')")
    public ResponseEntity<Map<String, Long>> summary() {
        return ResponseEntity.ok(deliveryService.dispatchSummary());
    }

    @Operation(summary = "Dispatcher: Assign or reassign driver + vehicle",
            description = "Valid for PENDING_ASSIGNMENT, AT_HUB (onward/redelivery trip) and DRIVER_ASSIGNED (reassignment). "
                    + "The vehicle category must satisfy the package's required category (decision matrix).")
    @PostMapping("/{deliveryId}/assign")
    @PreAuthorize("hasAuthority('ASSIGN_DRIVER')")
    public ResponseEntity<DeliveryResponseDTO> assign(@PathVariable String deliveryId,
                                                      @Valid @RequestBody DispatchAssignRequest request,
                                                      Authentication auth) {
        return ResponseEntity.ok(deliveryService.assignDriver(deliveryId, request, user(auth)));
    }

    @Operation(summary = "Dispatcher: Remove the driver from an uncollected package (driver unavailable)")
    @PostMapping("/{deliveryId}/unassign")
    @PreAuthorize("hasAuthority('ASSIGN_DRIVER')")
    public ResponseEntity<DeliveryResponseDTO> unassign(@PathVariable String deliveryId,
                                                        @Valid @RequestBody(required = false) DeliveryActionRequest request,
                                                        Authentication auth) {
        return ResponseEntity.ok(deliveryService.unassignDriver(deliveryId, request, user(auth)));
    }

    @Operation(summary = "Hub: Scan a package in at a hub (inspect / sort / onward transfer)",
            description = "Requires trackingCode (scanned) and hubName. Valid after pickup, while in transit or after a failed attempt.")
    @PostMapping("/{deliveryId}/hub-scan")
    @PreAuthorize("hasAuthority('ASSIGN_DRIVER')")
    public ResponseEntity<DeliveryResponseDTO> hubScan(@PathVariable String deliveryId,
                                                       @Valid @RequestBody DeliveryActionRequest request,
                                                       Authentication auth) {
        return ResponseEntity.ok(deliveryService.hubCheckIn(deliveryId, request, user(auth)));
    }

    @Operation(summary = "Dispatcher: Return package to sender (reason required)")
    @PostMapping("/{deliveryId}/return")
    @PreAuthorize("hasAuthority('ASSIGN_DRIVER')")
    public ResponseEntity<DeliveryResponseDTO> returnToSender(@PathVariable String deliveryId,
                                                              @Valid @RequestBody DeliveryActionRequest request,
                                                              Authentication auth) {
        return ResponseEntity.ok(deliveryService.returnToSender(deliveryId, request, user(auth)));
    }

    @Operation(summary = "Dispatcher: Cancel a package before collection (reason required)")
    @PostMapping("/{deliveryId}/cancel")
    @PreAuthorize("hasAuthority('ASSIGN_DRIVER')")
    public ResponseEntity<DeliveryResponseDTO> cancel(@PathVariable String deliveryId,
                                                      @Valid @RequestBody DeliveryActionRequest request,
                                                      Authentication auth) {
        return ResponseEntity.ok(deliveryService.cancelDelivery(deliveryId, request, user(auth)));
    }

    @Operation(summary = "Dispatcher: Reset a locked delivery code (after verifying the recipient)")
    @PostMapping("/{deliveryId}/reset-otp")
    @PreAuthorize("hasAuthority('ASSIGN_DRIVER')")
    public ResponseEntity<DeliveryResponseDTO> resetOtp(@PathVariable String deliveryId,
                                                        @Valid @RequestBody(required = false) DeliveryActionRequest request,
                                                        Authentication auth) {
        return ResponseEntity.ok(deliveryService.resetOtp(deliveryId, request, user(auth)));
    }

    // --- DRIVER ---

    @Operation(summary = "Driver: Confirm pickup by scanning the package QR / tracking number",
            description = "Body: { trackingCode, latitude?, longitude? }. The code must match the package tracking number.")
    @PostMapping("/{id}/pickup")
    @PreAuthorize("hasAuthority('DELIVER_PRODUCT')")
    public ResponseEntity<DeliveryResponseDTO> pickup(@PathVariable String id,
                                                      @Valid @RequestBody DeliveryActionRequest request,
                                                      Authentication auth) {
        return ResponseEntity.ok(deliveryService.confirmPickup(id, request, user(auth)));
    }

    @Operation(summary = "Driver: Confirm pickup (legacy alias of /pickup)")
    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority('DELIVER_PRODUCT')")
    public ResponseEntity<DeliveryResponseDTO> startDelivery(@PathVariable String id,
                                                             @RequestBody(required = false) DeliveryActionRequest request,
                                                             Authentication auth) {
        return ResponseEntity.ok(deliveryService.confirmPickup(id, request != null ? request : new DeliveryActionRequest(), user(auth)));
    }

    @Operation(summary = "Driver: Out for delivery (also used to re-attempt after a failed attempt)")
    @PostMapping("/{id}/in-transit")
    @PreAuthorize("hasAuthority('DELIVER_PRODUCT')")
    public ResponseEntity<DeliveryResponseDTO> inTransit(@PathVariable String id,
                                                         @RequestBody(required = false) DeliveryActionRequest request,
                                                         Authentication auth) {
        return ResponseEntity.ok(deliveryService.markInTransit(id, request, user(auth)));
    }

    @Operation(summary = "Driver: Report a failed delivery attempt",
            description = "reason: RECIPIENT_UNAVAILABLE, ADDRESS_NOT_FOUND, DAMAGED, DETAILS_MISMATCH, REFUSED or free text; note optional.")
    @PostMapping("/{id}/fail")
    @PreAuthorize("hasAuthority('DELIVER_PRODUCT')")
    public ResponseEntity<DeliveryResponseDTO> fail(@PathVariable String id,
                                                    @Valid @RequestBody DeliveryActionRequest request,
                                                    Authentication auth) {
        return ResponseEntity.ok(deliveryService.reportFailure(id, request, user(auth)));
    }

    @Operation(summary = "Driver: Report that the package could not be collected",
            description = "reason: PACKAGE_NOT_READY, SENDER_UNAVAILABLE, DRIVER_UNAVAILABLE, VEHICLE_ISSUE or free text. The package returns to the dispatch queue.")
    @PostMapping("/{id}/not-collected")
    @PreAuthorize("hasAuthority('DELIVER_PRODUCT')")
    public ResponseEntity<DeliveryResponseDTO> notCollected(@PathVariable String id,
                                                            @Valid @RequestBody DeliveryActionRequest request,
                                                            Authentication auth) {
        return ResponseEntity.ok(deliveryService.reportNotCollected(id, request, user(auth)));
    }

    @Operation(summary = "Driver: Complete delivery (recipient OTP + proof-of-delivery photo)")
    @PostMapping(value = "/{id}/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('DELIVER_PRODUCT')")
    public ResponseEntity<DeliveryResponseDTO> completeDelivery(@PathVariable String id,
                                                                @RequestParam("otp") String otp,
                                                                @RequestPart("image") MultipartFile image,
                                                                @RequestParam(value = "latitude", required = false) String latitude,
                                                                @RequestParam(value = "longitude", required = false) String longitude,
                                                                Authentication authentication) {
        return ResponseEntity.ok(deliveryService.verifyAndCompleteDelivery(id, otp, image, latitude, longitude, user(authentication)));
    }

    @Operation(summary = "Driver: Status update (legacy)", description = "Supports IN_TRANSIT only. Use the dedicated action endpoints.")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('DELIVER_PRODUCT')")
    public ResponseEntity<DeliveryResponseDTO> updateDeliveryStatus(@PathVariable String id,
                                                                    @RequestParam String status,
                                                                    Authentication authentication) {
        if (!"IN_TRANSIT".equalsIgnoreCase(status)) {
            throw new BusinessException("Use the dedicated endpoints: /pickup, /in-transit, /fail, /not-collected, /complete.");
        }
        return ResponseEntity.ok(deliveryService.markInTransit(id, null, user(authentication)));
    }

    @Operation(summary = "Driver: Optimized multi-stop route",
            description = "Ordered pickup and drop-off stops of all active jobs (pickups always precede their drop-off).")
    @GetMapping("/me/optimized-route")
    @PreAuthorize("hasAuthority('DELIVER_PRODUCT')")
    public ResponseEntity<List<RouteStopDTO>> getOptimizedRoute(Authentication auth) {
        return ResponseEntity.ok(navigationService.getOptimizedRoute(user(auth)));
    }

    @Operation(summary = "Driver: Navigation details of a package")
    @GetMapping("/{id}/route")
    @PreAuthorize("hasAuthority('DELIVER_PRODUCT')")
    public ResponseEntity<DeliveryResponseDTO> getDeliveryRoute(@PathVariable String id, Authentication authentication) {
        return ResponseEntity.ok(deliveryService.findDeliveryById(id, user(authentication)));
    }
}
