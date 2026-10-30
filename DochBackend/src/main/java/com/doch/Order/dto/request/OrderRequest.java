package com.doch.Order.dto.request;

import com.doch.Logistic.model.TransportType;
import com.doch.common.validation.ValidPaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request object to place a new marketplace order")
public class OrderRequest {

    @NotEmpty(message = "Order must contain at least one item")
    @Size(max = 50, message = "An order can contain at most 50 different products")
    private List<@Valid ProductBought> items;

    @NotBlank(message = "Delivery address is required")
    @Size(min = 5, max = 500, message = "Delivery address must be between 5 and 500 characters")
    @Schema(description = "Street address for delivery", example = "Bastos, Yaounde")
    private String deliveryAddress;

    @Schema(description = "Recipient phone number for the driver", example = "670000000")
    @Size(max = 30)
    private String contactPhone;

    @NotBlank(message = "Payment method is required")
    @ValidPaymentMethod
    @Schema(description = "Mobile money provider: MTN_MOMO or ORANGE_MONEY", example = "MTN_MOMO")
    private String paymentMethod;

    @Schema(description = "Optional: send the payment prompt directly to this mobile money number instead of returning a payment link",
            example = "670000000")
    @Pattern(regexp = "^$|^6[0-9]{8}$", message = "Phone must be a 9-digit Cameroon mobile number starting with 6.")
    private String paymentPhone;

    @NotNull(message = "Transport type is required")
    @Schema(description = "Type of transport needed", implementation = TransportType.class)
    private TransportType transportType = TransportType.SINGLE_PARCEL;
}