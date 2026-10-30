package com.doch.Order.controller;

import com.doch.Order.dto.request.OrderCancelRequest;
import com.doch.Order.dto.request.OrderPreviewRequest;
import com.doch.Order.dto.request.OrderRequest;
import com.doch.Order.dto.request.OrderReviewRequest;
import com.doch.Order.dto.response.OrderPreviewResponse;
import com.doch.Order.dto.response.OrderResponse;
import com.doch.Order.service.interfaces.OrderService;
import com.doch.User.model.User;
import com.doch.common.exceptions.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "2. Order Management", description = "Placing, paying, tracking, cancelling and reviewing marketplace orders.")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Buyer: Place a new order",
            description = "Reserves stock, creates one package per seller (with tracking number) and starts the payment. "
                    + "Pass `paymentPhone` to receive a mobile money prompt instead of a payment link.")
    @PostMapping
    @PreAuthorize("hasAuthority('PLACE_ORDER')")
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody OrderRequest request, Authentication auth) {
        return ResponseEntity.ok(orderService.placeOrder(request, (User) auth.getPrincipal()));
    }

    @Operation(summary = "Buyer: Preview order cost breakdown",
            description = "Calculates products + delivery fees per seller without creating an order.")
    @PostMapping("/preview")
    @PreAuthorize("hasAuthority('PLACE_ORDER')")
    public ResponseEntity<OrderPreviewResponse> previewOrderCost(@RequestBody @Valid OrderPreviewRequest request) {
        return ResponseEntity.ok(orderService.previewOrderCost(request));
    }

    @Operation(summary = "Buyer: Get my order history",
            description = "All orders of the logged-in customer with payment status, packages, tracking history and delivery OTPs.")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<OrderResponse>> getMyOrders(Authentication auth) {
        return ResponseEntity.ok(orderService.getMyOrders((User) auth.getPrincipal()));
    }

    @Operation(summary = "Seller: Get my incoming paid orders",
            description = "Paid orders containing the seller's products. Only the seller's own items and package are returned.")
    @GetMapping("/seller/me")
    @PreAuthorize("hasAuthority('ROLE_SELLER')")
    public ResponseEntity<List<OrderResponse>> getMySellerOrders(Authentication auth) {
        return ResponseEntity.ok(orderService.getMySellerOrders((User) auth.getPrincipal()));
    }

    @Operation(summary = "Buyer/Staff: Cancel an order",
            description = "Customers can cancel unpaid orders. Market/Super admins can also cancel paid orders before any package is collected "
                    + "(stock is released, seller earnings reversed and a refund is flagged).")
    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable String orderId,
                                                     @RequestBody(required = false) @Valid OrderCancelRequest request,
                                                     Authentication auth) {
        return ResponseEntity.ok(orderService.cancelOrder(orderId, (User) auth.getPrincipal(),
                request != null ? request.getReason() : null));
    }

    @Operation(summary = "Buyer: Rate a delivered order", description = "One review per order, only after delivery.")
    @PostMapping("/{orderId}/review")
    @PreAuthorize("hasAuthority('PLACE_ORDER')")
    public ResponseEntity<OrderResponse> review(@PathVariable String orderId,
                                                @RequestBody @Valid OrderReviewRequest request,
                                                Authentication auth) {
        return ResponseEntity.ok(orderService.submitReview(orderId, (User) auth.getPrincipal(), request));
    }

    // --- STAFF / ADMIN ENDPOINTS ---

    @Operation(summary = "Staff: List / search all orders (paginated)",
            description = """
                    Market admins, super admins and logistics staff. Sorted newest first. Filters (all optional, combinable):
                    - q: order ID, order reference (DO-...), package tracking number (DCH-...), customer email or name
                    - status: PENDING, PAYMENT_FAILED, PROCESSING, SHIPPED, DELIVERED, CANCELLED, RETURNED, ...
                    - sellerId / buyerId
                    - startDate / endDate: yyyy-MM-dd or ISO date-time
                    Response: { content, totalElements, totalPages, number, size }""")
    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('MANAGE_MARKET', 'MANAGE_SYSTEM', 'VIEW_ALL_DELIVERIES')")
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sellerId,
            @RequestParam(required = false) String buyerId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orderService.getAllOrdersFiltered(status, sellerId, buyerId, q,
                parseDateParam(startDate, false), parseDateParam(endDate, true), pageable));
    }

    private Date parseDateParam(String raw, boolean endOfDay) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim();
        try {
            LocalDate date = LocalDate.parse(value);
            LocalDateTime dateTime = endOfDay ? date.atTime(23, 59, 59) : date.atStartOfDay();
            return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException ignored) {
            // fall through to date-time parsing
        }
        try {
            return Date.from(LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException e) {
            throw new BusinessException("Invalid date filter: " + raw + ". Use yyyy-MM-dd or yyyy-MM-dd'T'HH:mm:ss");
        }
    }

    @Operation(summary = "Get order details by ID or reference",
            description = "Accessible by staff, the buyer, or the seller(s) involved (seller view is filtered). Includes tracking history.")
    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable String orderId, Authentication auth) {
        return ResponseEntity.ok(orderService.getOrderByIdSecure(orderId, (User) auth.getPrincipal()));
    }

    @Operation(summary = "Admin: Orders of a customer")
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyAuthority('MANAGE_MARKET', 'MANAGE_SYSTEM')")
    public ResponseEntity<List<OrderResponse>> getOrdersByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    @Operation(summary = "Admin: Orders by status")
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyAuthority('MANAGE_MARKET', 'MANAGE_SYSTEM')")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(@PathVariable String status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

    @Operation(summary = "Admin: Orders containing a product")
    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAnyAuthority('MANAGE_MARKET', 'MANAGE_SYSTEM')")
    public ResponseEntity<List<OrderResponse>> getOrdersByProductId(@PathVariable String productId) {
        return ResponseEntity.ok(orderService.getOrdersByProductId(productId));
    }

    @Operation(summary = "Admin: Orders of a seller")
    @GetMapping("/seller/{sellerId}")
    @PreAuthorize("hasAnyAuthority('MANAGE_MARKET', 'MANAGE_SYSTEM')")
    public ResponseEntity<List<OrderResponse>> getOrdersBySellerId(@PathVariable String sellerId) {
        return ResponseEntity.ok(orderService.getOrdersBySellerId(sellerId));
    }
}
