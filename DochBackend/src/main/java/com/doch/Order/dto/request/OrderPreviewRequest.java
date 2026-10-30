package com.doch.Order.dto.request;

import com.doch.Logistic.model.TransportType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request to preview order costs before placing")
public class OrderPreviewRequest {

    @NotEmpty(message = "Order must contain at least one item")
    private List<ProductBought> items;

    @NotBlank(message = "Delivery address is required")
    @Schema(description = "Street address for delivery", example = "Bastos, Yaoundé")
    private String deliveryAddress;

    @NotNull(message = "Transport type is required")
    @Schema(description = "Type of transport needed", implementation = TransportType.class)
    private TransportType transportType = TransportType.SINGLE_PARCEL;
}