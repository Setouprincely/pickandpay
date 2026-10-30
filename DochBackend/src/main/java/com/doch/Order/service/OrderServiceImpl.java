package com.doch.Order.service;

import com.doch.Catalog.model.Product;
import com.doch.Catalog.repository.ProductRepository;
import com.doch.Catalog.service.StoreMediaUsecase;
import com.doch.Fleet.model.VehicleCategory;
import com.doch.Fleet.service.VehicleDecisionMatrix;
import com.doch.Logistic.dto.DeliveryMapper;
import com.doch.Logistic.model.*;
import com.doch.Logistic.repository.DeliveryEventRepository;
import com.doch.Logistic.repository.DeliveryRepository;
import com.doch.Logistic.repository.DochMarketDeliveryRequestRepository;
import com.doch.Logistic.service.FulfilmentService;
import com.doch.Logistic.service.PricingService;
import com.doch.Logistic.service.TrackingEventService;
import com.doch.Notification.model.NotificationType;
import com.doch.Notification.service.NotificationService;
import com.doch.Order.dto.request.OrderPreviewRequest;
import com.doch.Order.dto.request.OrderRequest;
import com.doch.Order.dto.request.OrderReviewRequest;
import com.doch.Order.dto.request.ProductBought;
import com.doch.Order.dto.response.OrderItemResponse;
import com.doch.Order.dto.response.OrderPreviewResponse;
import com.doch.Order.dto.response.OrderResponse;
import com.doch.Order.model.*;
import com.doch.Order.repository.OrderItemRepository;
import com.doch.Order.repository.OrderRepository;
import com.doch.Order.repository.OrderReviewRepository;
import com.doch.Order.repository.OrderSpecifications;
import com.doch.Order.service.interfaces.OrderService;
import com.doch.Payment.dto.PaymentMapper;
import com.doch.Payment.dto.PaymentRequestDTO;
import com.doch.Payment.dto.PaymentResponseDTO;
import com.doch.Payment.model.Payment;
import com.doch.Payment.model.PaymentStatus;
import com.doch.Payment.repository.PaymentRepository;
import com.doch.Payment.service.PaymentService;
import com.doch.User.model.DriverProfile;
import com.doch.User.model.Seller;
import com.doch.User.model.User;
import com.doch.User.repository.DriverProfileRepository;
import com.doch.User.repository.SellerRepository;
import com.doch.common.Mapper;
import com.doch.common.ReferenceGenerator;
import com.doch.common.audit.AuditService;
import com.doch.common.exceptions.BusinessException;
import com.doch.common.exceptions.ResourceNotFoundException;
import com.doch.common.geocoding.DistanceService;
import com.doch.common.geocoding.GeocodingService;
import com.doch.common.settings.PricingSettings;
import com.doch.common.settings.PricingSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Set<String> STAFF_ORDER_AUTHORITIES = Set.of("MANAGE_SYSTEM", "MANAGE_MARKET", "VIEW_ALL_DELIVERIES");
    private static final Set<String> STAFF_CANCEL_AUTHORITIES = Set.of("MANAGE_SYSTEM", "MANAGE_MARKET");

    private final OrderItemRepository itemRepository;
    private final OrderRepository orderRepository;
    private final OrderReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final DochMarketDeliveryRequestRepository marketDeliveryRequestRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryEventRepository deliveryEventRepository;
    private final DriverProfileRepository driverProfileRepository;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PricingService pricingService;
    private final PricingSettingsService settingsService;
    private final GeocodingService geocodingService;
    private final DistanceService distanceService;
    private final Mapper mapper;
    private final DeliveryMapper deliveryMapper;
    private final SellerRepository sellerRepository;
    private final NotificationService notificationService;
    private final StoreMediaUsecase storeMediaUsecase;
    private final TrackingEventService trackingEventService;
    private final FulfilmentService fulfilmentService;
    private final AuditService auditService;

    @Value("${application.orders.unpaid-cancel-after-hours:48}")
    private long unpaidCancelAfterHours;

    // ================================================================== placement

    /** One pickup leg = all items of one seller. */
    private record SellerLeg(Seller seller, List<ProductBought> items, Map<String, Product> products) {
        double productTotal() {
            return items.stream().mapToDouble(i -> products.get(i.getProductId()).getPrice() * i.getQuantity()).sum();
        }

        Double totalWeightKg() {
            double total = 0;
            for (ProductBought i : items) {
                Double w = products.get(i.getProductId()).getWeightKg();
                if (w == null) return null;
                total += w * i.getQuantity();
            }
            return total;
        }

        Double maxDimensionCm() {
            return items.stream().map(i -> products.get(i.getProductId()))
                    .flatMap(p -> java.util.stream.Stream.of(p.getLengthCm(), p.getWidthCm(), p.getHeightCm()))
                    .filter(Objects::nonNull).max(Double::compare).orElse(null);
        }
    }

    @Override
    @Transactional
    public OrderResponse placeOrder(OrderRequest request, User client) {
        Map<String, Integer> quantities = mergeQuantities(request.getItems());

        // Lock product rows in a deterministic order: prevents overselling and deadlocks under concurrency.
        Map<String, Product> products = new LinkedHashMap<>();
        for (String productId : new TreeSet<>(quantities.keySet())) {
            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new BusinessException("Product not found: " + productId));
            assertPurchasable(product, quantities.get(productId), client);
            products.put(productId, product);
        }

        List<SellerLeg> legs = groupBySeller(quantities, products);
        Location destination = geocodingService.geocodeAddress(request.getDeliveryAddress())
                .orElseThrow(() -> new BusinessException("The delivery address could not be located. Please add more detail (district, city)."));
        TransportType transportType = request.getTransportType() != null ? request.getTransportType() : TransportType.SINGLE_PARCEL;
        PricingSettings settings = settingsService.getOrCreateSettings();

        Order order = Order.builder()
                .reference(newOrderReference())
                .customer(client)
                .deliveryAddress(request.getDeliveryAddress().trim())
                .contactPhone(StringUtils.hasText(request.getContactPhone()) ? request.getContactPhone().trim() : client.getTel())
                .transportType(transportType)
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>())
                .build();

        double productTotal = 0;
        double deliveryTotal = 0;
        List<Runnable> afterSave = new ArrayList<>();

        for (SellerLeg leg : legs) {
            Location pickup = geocodingService.geocodeAddress(leg.seller().getAddress())
                    .orElseThrow(() -> new BusinessException("Could not locate the warehouse of seller " + leg.seller().getName() + "."));
            DistanceService.DistanceResult route = distanceService.getRoadDistance(
                            pickup.getLatitude(), pickup.getLongitude(), destination.getLatitude(), destination.getLongitude())
                    .orElseThrow(() -> new BusinessException("Road distance calculation failed for seller " + leg.seller().getName() + "."));
            double legFee = pricingService.calculateDeliveryPrice(route.distanceKm(), transportType);
            Double legWeight = leg.totalWeightKg();
            Double legMaxDim = leg.maxDimensionCm();
            VehicleCategory required = VehicleDecisionMatrix.requiredCategory(legWeight, legMaxDim, transportType,
                    settings.getBicycleMaxWeightKg(), settings.getBicycleMaxDimensionCm(), settings.getKTruckMaxWeightKg());

            for (ProductBought line : leg.items()) {
                Product product = products.get(line.getProductId());
                OrderItem item = OrderItem.builder()
                        .order(order)
                        .product(product)
                        .quantity(line.getQuantity())
                        .unite_priced(product.getPrice())
                        .total_price(product.getPrice() * line.getQuantity())
                        .build();
                order.getItems().add(item);
                productTotal += item.getTotal_price();

                product.setStockQuantity(product.getStockQuantity() - line.getQuantity());
                product.setQuantitySold(product.getQuantitySold() + line.getQuantity());
            }
            deliveryTotal += legFee;

            Location receiver = Location.builder().latitude(destination.getLatitude()).longitude(destination.getLongitude()).build();
            DochMarketDeliveryRequest bridge = DochMarketDeliveryRequest.builder()
                    .seller(leg.seller())
                    .order(order)
                    .pickupLocation(pickup)
                    .receiverLocation(receiver)
                    .price(legFee)
                    .deliveryNote("Pickup from " + leg.seller().getName() + " (" + leg.seller().getAddress() + "). Mode: " + transportType)
                    .build();
            double durationMinutes = route.durationMinutes();
            afterSave.add(() -> {
                marketDeliveryRequestRepository.save(bridge);
                Delivery delivery = deliveryRepository.save(Delivery.builder()
                        .marketDeliveryRequest(bridge)
                        .trackingNumber(newTrackingNumber())
                        .status(DeliveryStatus.PENDING_PAYMENT)
                        .packageWeightKg(legWeight)
                        .packageMaxDimensionCm(legMaxDim)
                        .requiredVehicleCategory(required)
                        .estimatedDeliveryTime(LocalDateTime.now().plusMinutes((long) durationMinutes + 120))
                        .otpFailedAttempts(0)
                        .build());
                trackingEventService.record(delivery, TrackingEventService.CREATED, null, DeliveryStatus.PENDING_PAYMENT,
                        "Order placed - awaiting payment",
                        String.format("Leg for seller %s, %s kg, requires %s", leg.seller().getName(),
                                legWeight != null ? String.format("%.2f", legWeight) : "unknown", required), client);
            });
        }

        order.setProductTotal(productTotal);
        order.setDeliveryFee(deliveryTotal);
        order.setTotalAmount(productTotal + deliveryTotal);
        Order saved = orderRepository.save(order);
        itemRepository.saveAll(saved.getItems());
        productRepository.saveAll(products.values());
        afterSave.forEach(Runnable::run);

        auditService.record(client, "ORDER_PLACED", "ORDER", saved.getId(),
                String.format("Order %s: %d item line(s), %d package(s), %.0f XAF", saved.getReference(),
                        saved.getItems().size(), legs.size(), saved.getTotalAmount()));
        notificationService.sendNotificationToRole("ROLE_MARKET_ADMIN", NotificationType.ORDER_PLACED,
                String.format("New order %s (%.0f XAF) placed by %s %s - awaiting payment.",
                        saved.getReference(), saved.getTotalAmount(), client.getFirstName(), client.getLastName()));

        PaymentResponseDTO payment = paymentService.initiatePayment(PaymentRequestDTO.builder()
                .orderId(saved.getId())
                .paymentMethod(request.getPaymentMethod())
                .phone(request.getPaymentPhone())
                .build(), client);

        OrderResponse response = toView(saved, client, Perspective.CUSTOMER, false);
        response.setPaymentDetails(payment);
        response.setStatus(saved.getStatus().name());
        return response;
    }

    @Override
    @Transactional
    public OrderPreviewResponse previewOrderCost(OrderPreviewRequest request) {
        Map<String, Integer> quantities = mergeQuantities(request.getItems());
        Map<String, Product> products = new LinkedHashMap<>();
        for (String productId : quantities.keySet()) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new BusinessException("Product not found: " + productId));
            assertPurchasable(product, quantities.get(productId), null);
            products.put(productId, product);
        }
        List<SellerLeg> legs = groupBySeller(quantities, products);
        Location destination = geocodingService.geocodeAddress(request.getDeliveryAddress())
                .orElseThrow(() -> new BusinessException("The delivery address could not be located. Please add more detail (district, city)."));
        TransportType type = request.getTransportType() != null ? request.getTransportType() : TransportType.SINGLE_PARCEL;

        double totalProducts = 0;
        double totalDelivery = 0;
        List<OrderPreviewResponse.SellerCostBreakdown> breakdowns = new ArrayList<>();
        for (SellerLeg leg : legs) {
            Location pickup = geocodingService.geocodeAddress(leg.seller().getAddress())
                    .orElseThrow(() -> new BusinessException("Could not locate the warehouse of seller " + leg.seller().getName() + "."));
            DistanceService.DistanceResult route = distanceService.getRoadDistance(
                            pickup.getLatitude(), pickup.getLongitude(), destination.getLatitude(), destination.getLongitude())
                    .orElseThrow(() -> new BusinessException("Road distance calculation failed for seller " + leg.seller().getName() + "."));
            double fee = pricingService.calculateDeliveryPrice(route.distanceKm(), type);
            List<OrderItemResponse> items = leg.items().stream().map(i -> {
                Product p = leg.products().get(i.getProductId());
                return OrderItemResponse.builder()
                        .productId(p.getId())
                        .productName(p.getName())
                        .productImageUrl(firstImageUrl(p))
                        .sellerId(leg.seller().getId())
                        .sellerName(leg.seller().getName())
                        .quantity(i.getQuantity())
                        .unite_price(p.getPrice())
                        .total_price(p.getPrice() * i.getQuantity())
                        .weightKg(p.getWeightKg())
                        .build();
            }).toList();
            double subtotal = leg.productTotal();
            breakdowns.add(OrderPreviewResponse.SellerCostBreakdown.builder()
                    .sellerId(leg.seller().getId())
                    .sellerName(leg.seller().getName())
                    .sellerAddress(leg.seller().getAddress())
                    .productSubtotal(subtotal)
                    .deliveryFee(fee)
                    .sellerTotal(subtotal + fee)
                    .distanceKm(route.distanceKm())
                    .items(items)
                    .build());
            totalProducts += subtotal;
            totalDelivery += fee;
        }
        return OrderPreviewResponse.builder()
                .productTotal(totalProducts)
                .totalDeliveryFee(totalDelivery)
                .totalAmount(totalProducts + totalDelivery)
                .transportType(type)
                .deliveryAddress(request.getDeliveryAddress())
                .sellerBreakdowns(breakdowns)
                .build();
    }

    private Map<String, Integer> mergeQuantities(List<ProductBought> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new BusinessException("Order must contain at least one item.");
        }
        Map<String, Integer> merged = new LinkedHashMap<>();
        for (ProductBought line : lines) {
            if (line.getProductId() == null || line.getQuantity() == null || line.getQuantity() < 1) {
                throw new BusinessException("Each item needs a product and a quantity of at least 1.");
            }
            merged.merge(line.getProductId(), line.getQuantity(), Integer::sum);
        }
        return merged;
    }

    private void assertPurchasable(Product product, int quantity, User buyer) {
        if (!product.isPubliclyVisible()) {
            throw new BusinessException("'" + product.getName() + "' is not available for purchase.");
        }
        if (product.getSeller().getAddress() == null || product.getSeller().getAddress().isBlank()) {
            throw new BusinessException("'" + product.getName() + "' has no pickup address configured by its seller.");
        }
        if (buyer != null && product.getSeller().getUser() != null
                && product.getSeller().getUser().getId().equals(buyer.getId())) {
            throw new BusinessException("You cannot order your own product '" + product.getName() + "'.");
        }
        if (product.getStockQuantity() < quantity) {
            throw new BusinessException("Insufficient stock for '" + product.getName() + "': only "
                    + product.getStockQuantity() + " left.");
        }
    }

    private List<SellerLeg> groupBySeller(Map<String, Integer> quantities, Map<String, Product> products) {
        Map<String, SellerLeg> legs = new LinkedHashMap<>();
        quantities.forEach((productId, qty) -> {
            Product product = products.get(productId);
            Seller seller = product.getSeller();
            legs.computeIfAbsent(seller.getId(), id -> new SellerLeg(seller, new ArrayList<>(), products))
                    .items().add(new ProductBought(productId, qty));
        });
        return new ArrayList<>(legs.values());
    }

    private String newOrderReference() {
        for (int i = 0; i < 10; i++) {
            String ref = ReferenceGenerator.orderReference();
            if (!orderRepository.existsByReference(ref)) return ref;
        }
        throw new IllegalStateException("Could not generate a unique order reference");
    }

    private String newTrackingNumber() {
        for (int i = 0; i < 10; i++) {
            String ref = ReferenceGenerator.trackingNumber();
            if (!deliveryRepository.existsByTrackingNumber(ref)) return ref;
        }
        throw new IllegalStateException("Could not generate a unique tracking number");
    }

    // ================================================================== queries

    private enum Perspective { CUSTOMER, SELLER, STAFF }

    @Override
    @Transactional
    public OrderResponse getOrderByIdSecure(String orderIdOrReference, User currentUser) {
        Order order = findOrder(orderIdOrReference);
        boolean isStaff = hasAny(currentUser, STAFF_ORDER_AUTHORITIES);
        boolean isBuyer = order.getCustomer().getId().equals(currentUser.getId());
        Optional<Seller> seller = sellerRepository.findByUserId(currentUser.getId()).stream().findFirst();
        boolean isInvolvedSeller = seller.isPresent() && order.getItems().stream()
                .anyMatch(item -> item.getProduct().getSeller() != null
                        && item.getProduct().getSeller().getId().equals(seller.get().getId()));

        if (!isStaff && !isBuyer && !isInvolvedSeller) {
            throw new AccessDeniedException("Access Denied.");
        }
        if (isBuyer) return toView(order, currentUser, Perspective.CUSTOMER, true);
        if (isStaff) return toView(order, currentUser, Perspective.STAFF, true);
        if (!isPaid(order)) {
            throw new AccessDeniedException("Sellers can only view paid orders.");
        }
        return toSellerView(order, seller.get());
    }

    private Order findOrder(String idOrReference) {
        if (idOrReference == null || idOrReference.isBlank()) {
            throw new ResourceNotFoundException("Order not found");
        }
        String key = idOrReference.trim();
        return orderRepository.findById(key)
                .or(() -> orderRepository.findByReferenceIgnoreCase(key))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + key));
    }

    @Override
    @Transactional
    public Page<OrderResponse> getAllOrdersFiltered(String status, String sellerId, String buyerId, String query,
                                                    Date startDate, Date endDate, Pageable pageable) {
        OrderStatus statusEnum = parseStatus(status);
        Specification<Order> spec = OrderSpecifications.hasStatus(statusEnum)
                .and(OrderSpecifications.hasSeller(StringUtils.hasText(sellerId) ? sellerId : null))
                .and(OrderSpecifications.hasBuyer(StringUtils.hasText(buyerId) ? buyerId : null))
                .and(OrderSpecifications.matchesQuery(query))
                .and(OrderSpecifications.createdAfter(startDate))
                .and(OrderSpecifications.createdBefore(endDate));
        User viewer = AuditService.currentUser();
        return orderRepository.findAll(spec, pageable).map(order -> toView(order, viewer, Perspective.STAFF, false));
    }

    private OrderStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) return null;
        try {
            return OrderStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid order status: " + status + ". Allowed values: " + Arrays.toString(OrderStatus.values()));
        }
    }

    @Override
    @Transactional
    public List<OrderResponse> getOrdersByUserId(String userId) {
        return orderRepository.findByCustomerId(userId).stream()
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .map(o -> toView(o, null, Perspective.STAFF, false)).toList();
    }

    @Override
    @Transactional
    public List<OrderResponse> getOrdersByStatus(String status) {
        OrderStatus parsed = parseStatus(status);
        if (parsed == null) throw new BusinessException("Status is required.");
        return orderRepository.findByStatus(parsed).stream()
                .map(o -> toView(o, null, Perspective.STAFF, false)).toList();
    }

    @Override
    @Transactional
    public List<OrderResponse> getOrdersByProductId(String productId) {
        return orderRepository.findByItemsProductId(productId).stream()
                .map(o -> toView(o, null, Perspective.STAFF, false)).toList();
    }

    @Override
    @Transactional
    public List<OrderResponse> getOrdersBySellerId(String sellerId) {
        return orderRepository.findOrdersBySeller(sellerId).stream()
                .map(o -> toView(o, null, Perspective.STAFF, false)).toList();
    }

    @Override
    @Transactional
    public List<OrderResponse> getMySellerOrders(User currentUser) {
        Seller seller = sellerRepository.findByUserId(currentUser.getId()).stream().findFirst()
                .orElseThrow(() -> new BusinessException("You do not have a Seller Profile."));
        return orderRepository.findOrdersBySeller(seller.getId()).stream()
                .filter(this::isPaid)
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .map(order -> toSellerView(order, seller))
                .toList();
    }

    @Override
    @Transactional
    public List<OrderResponse> getMyOrders(User currentUser) {
        return orderRepository.findByCustomerId(currentUser.getId()).stream()
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .map(order -> toView(order, currentUser, Perspective.CUSTOMER, true))
                .toList();
    }

    private boolean isPaid(Order order) {
        return order.getPaidAt() != null
                || paymentRepository.findByOrder(order).map(p -> p.getStatus() == PaymentStatus.COMPLETED).orElse(false);
    }

    // ================================================================== cancellation

    @Override
    @Transactional
    public OrderResponse cancelOrder(String orderId, User actor, String reason) {
        Order order = orderRepository.findByIdForUpdate(findOrder(orderId).getId()).orElseThrow();
        boolean isOwner = order.getCustomer().getId().equals(actor.getId());
        boolean isStaff = hasAny(actor, STAFF_CANCEL_AUTHORITIES);
        if (!isOwner && !isStaff) {
            throw new AccessDeniedException("You cannot cancel this order.");
        }

        // Never cancel on stale data: refresh pending payments from the provider first.
        if (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.PAYMENT_FAILED) {
            paymentService.reconcileOrderPayments(order);
        }

        List<Delivery> legs = deliveryRepository.findByMarketDeliveryRequest_Order(order);
        boolean paid = order.getPaidAt() != null;
        if (paid && !isStaff) {
            throw new BusinessException("Order " + order.getReference() + " is already paid. Please contact DOCH support to cancel it.");
        }
        if (!EnumSet.of(OrderStatus.PENDING, OrderStatus.PAYMENT_FAILED, OrderStatus.PROCESSING).contains(order.getStatus())) {
            throw new BusinessException("Order " + order.getReference() + " cannot be cancelled in status " + order.getStatus() + ".");
        }
        if (legs.stream().anyMatch(l -> !DeliveryStateMachine.canTransition(l.getStatus(), DeliveryStatus.CANCELLED))) {
            throw new BusinessException("A package of this order has already been collected. Use the return process instead.");
        }

        String why = StringUtils.hasText(reason) ? reason.trim() : (isOwner ? "Cancelled by customer" : "Cancelled by DOCH");
        paymentService.cancelPendingPayments(order, why);

        for (Delivery leg : legs) {
            DeliveryStatus from = leg.getStatus();
            leg.setStatus(DeliveryStatus.CANCELLED);
            leg.setFailureReason(why);
            deliveryRepository.save(leg);
            trackingEventService.record(leg, TrackingEventService.STATUS_CHANGE, from, DeliveryStatus.CANCELLED,
                    TrackingEventService.describe(DeliveryStatus.CANCELLED), why, actor);
            fulfilmentService.afterDeliveryStatusChange(leg, from, actor);
        }

        OrderStatus previous = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancellationReason(why);
        orderRepository.save(order);
        auditService.record(actor, "ORDER_CANCELLED", "ORDER", order.getId(), previous + " -> CANCELLED: " + why);

        notificationService.sendNotification(order.getCustomer(), NotificationType.ORDER_CANCELLED,
                "Order " + order.getReference() + " has been cancelled." + (paid ? " Your refund is being processed." : ""));
        if (paid) {
            auditService.record(actor, "PAYMENT_REFUND_REQUIRED", "ORDER", order.getId(),
                    String.format("Paid order %s cancelled: refund %.0f XAF to the customer via payout.", order.getReference(), order.getTotalAmount()));
            notificationService.sendNotificationToRole("ROLE_SUPER_ADMIN", NotificationType.REFUND_PROCESSED,
                    String.format("REFUND REQUIRED: paid order %s (%.0f XAF) was cancelled.", order.getReference(), order.getTotalAmount()));
            legs.stream().map(l -> l.getMarketDeliveryRequest().getSeller()).filter(Objects::nonNull).distinct()
                    .forEach(s -> notificationService.sendNotification(s.getUser(), NotificationType.ORDER_CANCELLED,
                            "Order " + order.getReference() + " was cancelled. Do not ship it."));
        }
        return toView(order, actor, isOwner ? Perspective.CUSTOMER : Perspective.STAFF, true);
    }

    @Override
    @Transactional
    public int cancelStaleUnpaidOrders() {
        Date before = Date.from(LocalDateTime.now().minusHours(unpaidCancelAfterHours).atZone(ZoneId.systemDefault()).toInstant());
        List<Order> stale = orderRepository.findByStatusInCreatedBefore(List.of(OrderStatus.PENDING, OrderStatus.PAYMENT_FAILED), before);
        int cancelled = 0;
        for (Order candidate : stale) {
            Order order = orderRepository.findByIdForUpdate(candidate.getId()).orElse(null);
            if (order == null) continue;
            paymentService.reconcileOrderPayments(order);
            if (order.getPaidAt() != null) continue;
            if (paymentRepository.findAllByOrderOrderByCreatedAtDesc(order).stream()
                    .anyMatch(p -> p.getStatus() == PaymentStatus.PENDING && "DIRECT".equals(p.getPaymentFlow()))) {
                continue; // a mobile money prompt may still be approved
            }
            paymentService.cancelPendingPayments(order, "Order not paid in time");
            for (Delivery leg : deliveryRepository.findByMarketDeliveryRequest_Order(order)) {
                if (!DeliveryStateMachine.canTransition(leg.getStatus(), DeliveryStatus.CANCELLED)) continue;
                DeliveryStatus from = leg.getStatus();
                leg.setStatus(DeliveryStatus.CANCELLED);
                leg.setFailureReason("Order not paid in time");
                deliveryRepository.save(leg);
                trackingEventService.record(leg, TrackingEventService.STATUS_CHANGE, from, DeliveryStatus.CANCELLED,
                        TrackingEventService.describe(DeliveryStatus.CANCELLED), "Automatic cancellation of unpaid order", null);
                fulfilmentService.afterDeliveryStatusChange(leg, from, null);
            }
            order.setStatus(OrderStatus.CANCELLED);
            order.setCancelledAt(LocalDateTime.now());
            order.setCancellationReason("Order not paid within " + unpaidCancelAfterHours + " hours");
            orderRepository.save(order);
            auditService.record(null, "ORDER_AUTO_CANCELLED", "ORDER", order.getId(), order.getCancellationReason());
            notificationService.sendNotification(order.getCustomer(), NotificationType.ORDER_CANCELLED,
                    "Order " + order.getReference() + " was cancelled because it was not paid in time.");
            cancelled++;
        }
        return cancelled;
    }

    // ================================================================== reviews

    @Override
    @Transactional
    public OrderResponse submitReview(String orderId, User customer, OrderReviewRequest request) {
        Order order = findOrder(orderId);
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new AccessDeniedException("You can only review your own orders.");
        }
        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException("You can rate an order once it has been delivered.");
        }
        if (reviewRepository.existsByOrder(order)) {
            throw new BusinessException("You have already reviewed this order.");
        }
        reviewRepository.save(OrderReview.builder()
                .order(order)
                .customer(customer)
                .rating(request.getRating())
                .deliveryRating(request.getDeliveryRating())
                .comment(StringUtils.hasText(request.getComment()) ? request.getComment().trim() : null)
                .build());

        if (request.getDeliveryRating() != null) {
            deliveryRepository.findByMarketDeliveryRequest_Order(order).stream()
                    .map(Delivery::getDriver).filter(Objects::nonNull).distinct()
                    .forEach(this::refreshDriverRating);
        }
        auditService.record(customer, "ORDER_REVIEWED", "ORDER", order.getId(),
                "Rating " + request.getRating() + (request.getDeliveryRating() != null ? ", delivery " + request.getDeliveryRating() : ""));
        order.getItems().stream().map(i -> i.getProduct().getSeller()).filter(Objects::nonNull).distinct()
                .forEach(s -> notificationService.sendNotification(s.getUser(), NotificationType.SYSTEM_ANNOUNCEMENT,
                        "Order " + order.getReference() + " received a " + request.getRating() + "/5 rating."));
        return toView(order, customer, Perspective.CUSTOMER, true);
    }

    private void refreshDriverRating(User driver) {
        Double average = reviewRepository.averageDeliveryRatingForDriver(driver.getId());
        if (average == null) return;
        driverProfileRepository.findByUser(driver).ifPresent(profile -> {
            profile.setRating(Math.round(average * 10.0) / 10.0);
            driverProfileRepository.save(profile);
        });
    }

    // ================================================================== view assembly

    private OrderResponse toView(Order order, User viewer, Perspective perspective, boolean includeEvents) {
        OrderResponse response = mapper.toOrderResponse(order);
        Payment payment = paymentRepository.findByOrder(order).orElse(null);
        response.setPaymentDetails(paymentMapper.toDto(payment));
        if (perspective == Perspective.STAFF) {
            response.setCustomerEmail(order.getCustomer().getEmail());
            if (response.getContactPhone() == null) response.setContactPhone(order.getCustomer().getTel());
        }

        List<Delivery> legs = deliveryRepository.findByMarketDeliveryRequest_Order(order);
        boolean ownerView = perspective == Perspective.CUSTOMER;
        response.setDeliveryLegs(legs.stream().map(d -> legSummary(d, ownerView, perspective == Perspective.STAFF, includeEvents)).toList());

        reviewRepository.findByOrder(order).ifPresent(r -> response.setReview(OrderResponse.ReviewResponse.builder()
                .rating(r.getRating())
                .deliveryRating(r.getDeliveryRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build()));

        boolean unpaid = order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.PAYMENT_FAILED;
        if (perspective == Perspective.CUSTOMER) {
            response.setCanPay(unpaid);
            response.setCanCancel(unpaid);
            response.setCanReview((order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.COMPLETED)
                    && response.getReview() == null);
        } else if (perspective == Perspective.STAFF) {
            boolean cancellable = (unpaid || order.getStatus() == OrderStatus.PROCESSING)
                    && legs.stream().allMatch(l -> DeliveryStateMachine.canTransition(l.getStatus(), DeliveryStatus.CANCELLED));
            response.setCanCancel(viewer != null && hasAny(viewer, STAFF_CANCEL_AUTHORITIES) && cancellable);
        }
        return response;
    }

    private OrderResponse.DeliveryLegSummary legSummary(Delivery d, boolean ownerView, boolean staffView, boolean includeEvents) {
        Seller seller = d.getMarketDeliveryRequest().getSeller();
        OrderResponse.DeliveryLegSummary.DeliveryLegSummaryBuilder b = OrderResponse.DeliveryLegSummary.builder()
                .deliveryId(d.getId())
                .trackingNumber(d.getTrackingNumber())
                .sellerId(seller != null ? seller.getId() : null)
                .sellerName(seller != null ? seller.getName() : "Unknown seller")
                .status(d.getStatus() != null ? d.getStatus().name() : "UNKNOWN")
                .requiredVehicleCategory(d.getRequiredVehicleCategory() != null ? d.getRequiredVehicleCategory().name() : null)
                .packageWeightKg(d.getPackageWeightKg())
                .estimatedDeliveryTime(d.getEstimatedDeliveryTime())
                .deliveredAt(d.getEndTime())
                .failureReason(d.getFailureReason())
                .deliveryFee(d.getMarketDeliveryRequest().getPrice());
        if (d.getDriver() != null) {
            b.driverName(d.getDriver().getFirstName() + " " + d.getDriver().getLastName());
            b.driverPhone(d.getDriver().getTel());
        }
        if (d.getVehicle() != null) {
            b.vehicleName(d.getVehicle().getName())
                    .vehiclePlate(d.getVehicle().getPlateNumber())
                    .vehicleCategory(d.getVehicle().getCategory() != null ? d.getVehicle().getCategory().name() : null);
        }
        if (ownerView && d.getStatus() != DeliveryStatus.DELIVERED && !DeliveryStateMachine.isTerminal(d.getStatus())) {
            b.deliveryOtp(d.getDeliveryOtp());
        }
        if (includeEvents) {
            b.events(deliveryMapper.toEventDtos(deliveryEventRepository.findByDeliveryOrderByOccurredAtAsc(d), staffView));
        }
        return b.build();
    }

    /** Seller sees only their own items and package; no delivery fee, payment or customer contact data. */
    private OrderResponse toSellerView(Order order, Seller seller) {
        OrderResponse response = mapper.toOrderResponse(order);
        List<OrderItemResponse> mine = response.getItems().stream()
                .filter(i -> seller.getId().equals(i.getSellerId())).toList();
        response.setItems(mine);
        double myTotal = mine.stream().mapToDouble(OrderItemResponse::getTotal_price).sum();
        response.setProductTotal(myTotal);
        response.setTotalAmount(myTotal);
        response.setDeliveryFee(null);
        response.setPaymentDetails(null);
        response.setContactPhone(null);
        response.setDeliveryAddress(null);

        deliveryRepository.findByMarketDeliveryRequest_Order(order).stream()
                .filter(d -> d.getMarketDeliveryRequest().getSeller() != null
                        && d.getMarketDeliveryRequest().getSeller().getId().equals(seller.getId()))
                .findFirst()
                .ifPresent(d -> {
                    response.setSellerDeliveryId(d.getId());
                    response.setSellerDeliveryStatus(d.getStatus().name());
                    response.setSellerTrackingNumber(d.getTrackingNumber());
                    OrderResponse.DeliveryLegSummary leg = legSummary(d, false, false, true);
                    leg.setDeliveryFee(null);
                    leg.setDriverPhone(d.getDriver() != null ? d.getDriver().getTel() : null);
                    response.setDeliveryLegs(List.of(leg));
                });
        return response;
    }

    private String firstImageUrl(Product product) {
        if (product.getImages() == null || product.getImages().isEmpty()) return null;
        try {
            return storeMediaUsecase.getPresignedUrl(product.getImages().get(0).getImageUrl());
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean hasAny(User user, Set<String> authorities) {
        return user != null && user.getAuthorities().stream().anyMatch(a -> authorities.contains(a.getAuthority()));
    }
}
