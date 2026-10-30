package com.doch.Order.dto.response;

import com.doch.Logistic.model.TransportType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Price breakdown preview for order before placement")
public class OrderPreviewResponse {

    @Schema(description = "Sum of all product prices", example = "10000.0")
    private double productTotal;

    @Schema(description = "Total cost for delivery/logistics across all sellers", example = "1500.0")
    private double totalDeliveryFee;

    @Schema(description = "The final amount to be charged (Product + Delivery)", example = "11500.0")
    private double totalAmount;

    @Schema(description = "The mode of transport selected")
    private TransportType transportType;

    @Schema(description = "The destination address provided", example = "Bastos, Yaoundé")
    private String deliveryAddress;

    @Schema(description = "Breakdown per seller showing what each seller's products + their delivery cost")
    private List<SellerCostBreakdown> sellerBreakdowns;

    @Data
    @Builder
    @Schema(description = "Cost breakdown for each individual seller")
    public static class SellerCostBreakdown {
        private String sellerId;
        private String sellerName;
        private String sellerAddress;
        private double productSubtotal;
        private double deliveryFee;
        private double sellerTotal;
        private double distanceKm;
        private List<OrderItemResponse> items;
    }
}