package com.doch.Order.dto.response;

import com.doch.Logistic.dto.DeliveryEventDTO;
import com.doch.Logistic.model.TransportType;
import com.doch.Payment.dto.PaymentResponseDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Order view. Fields are filtered according to the caller's role (customer, seller, staff).")
public class OrderResponse {
    private String orderId;

    @Schema(description = "Human-friendly unique order reference", example = "DO-7K3M9Q2X")
    private String reference;

    private String status;

    @Schema(description = "Seller view only: the package (delivery) ID for this seller's portion of the order")
    private String sellerDeliveryId;
    @Schema(description = "Seller view only: the status of this seller's package (e.g., PENDING_PREPARATION)")
    private String sellerDeliveryStatus;
    @Schema(description = "Seller view only: tracking number of this seller's package")
    private String sellerTrackingNumber;

    @Schema(description = "Sum of all product prices", example = "10000.0")
    private double productTotal;
    @Schema(description = "Total cost for delivery/logistics (hidden from sellers)", example = "1500.0")
    private Double deliveryFee;
    @Schema(description = "The final amount charged (Product + Delivery)", example = "11500.0")
    private double totalAmount;

    private TransportType transportType;
    private String deliveryAddress;
    private String contactPhone;

    private List<OrderItemResponse> items;
    private String customerName;
    private String customerId;
    private String customerEmail;

    private Date createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;

    private PaymentResponseDTO paymentDetails;

    @Schema(description = "All packages (one per seller) of this order")
    private List<DeliveryLegSummary> deliveryLegs;

    private ReviewResponse review;

    @Schema(description = "Customer may still retry payment")
    private Boolean canPay;
    @Schema(description = "Caller may cancel this order now")
    private Boolean canCancel;
    @Schema(description = "Customer may rate this order")
    private Boolean canReview;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DeliveryLegSummary {
        private String deliveryId;
        private String trackingNumber;
        private String sellerId;
        private String sellerName;
        private String status;
        private String driverName;
        private String driverPhone;
        private String vehicleName;
        private String vehiclePlate;
        private String vehicleCategory;
        private String requiredVehicleCategory;
        private Double packageWeightKg;
        private Double deliveryFee;
        private LocalDateTime estimatedDeliveryTime;
        private LocalDateTime deliveredAt;
        private String failureReason;
        @Schema(description = "Only returned to the customer who owns the order")
        private String deliveryOtp;
        private List<DeliveryEventDTO> events;
    }

    @Data
    @Builder
    public static class ReviewResponse {
        private Integer rating;
        private Integer deliveryRating;
        private String comment;
        private Date createdAt;
    }
}