package com.doch.Order.service.interfaces;

import com.doch.Order.dto.request.OrderPreviewRequest;
import com.doch.Order.dto.request.OrderRequest;
import com.doch.Order.dto.request.OrderReviewRequest;
import com.doch.Order.dto.response.OrderPreviewResponse;
import com.doch.Order.dto.response.OrderResponse;
import com.doch.User.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;

public interface OrderService {

    OrderResponse placeOrder(OrderRequest request, User client);

    OrderPreviewResponse previewOrderCost(OrderPreviewRequest request);

    /** Accepts the order UUID or its human reference (DO-XXXXXXXX). */
    OrderResponse getOrderByIdSecure(String orderIdOrReference, User currentUser);

    Page<OrderResponse> getAllOrdersFiltered(String status, String sellerId, String buyerId, String query,
                                             Date startDate, Date endDate, Pageable pageable);

    List<OrderResponse> getOrdersByUserId(String userId);

    List<OrderResponse> getOrdersByStatus(String status);

    List<OrderResponse> getOrdersByProductId(String productId);

    List<OrderResponse> getOrdersBySellerId(String sellerId);

    List<OrderResponse> getMySellerOrders(User currentUser);

    List<OrderResponse> getMyOrders(User currentUser);

    OrderResponse cancelOrder(String orderId, User actor, String reason);

    OrderResponse submitReview(String orderId, User customer, OrderReviewRequest request);

    /** Cancels unpaid orders older than the configured threshold and releases their stock. */
    int cancelStaleUnpaidOrders();
}
