package com.doch.Payment.dto;

import com.doch.common.validation.OneOf;
import com.doch.common.validation.ValidPaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@OneOf(fields = {"orderId", "xenderRequestId"}, message = "Exactly one of 'orderId' or 'xenderRequestId' must be provided.")
public class PaymentRequestDTO {

    @Schema(description = "Ignored for security: the amount is always taken from the order / delivery request on the server.")
    private Double amount;

    @NotBlank(message = "Payment method is required.")
    @ValidPaymentMethod
    private String paymentMethod;

    @Schema(description = "Optional Cameroon mobile money number (9 digits, e.g. 670000000). When provided, the payment "
            + "prompt is sent directly to this phone instead of returning a hosted payment link.")
    @Pattern(regexp = "^$|^6[0-9]{8}$", message = "Phone must be a 9-digit Cameroon mobile number starting with 6.")
    private String phone;

    private String orderId;
    private String xenderRequestId;
}