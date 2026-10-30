package com.doch.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;

public class PaymentMethodValidator implements ConstraintValidator<ValidPaymentMethod, String> {

    // Define the allowed payment methods here
    private final List<String> allowedMethods = Arrays.asList("MTN_MOMO", "ORANGE_MONEY");

    @Override
    public boolean isValid(String paymentMethod, ConstraintValidatorContext context) {
        // The value is considered valid if it's null (to be caught by @NotBlank)
        // or if it's in our list of allowed methods.
        return paymentMethod == null || allowedMethods.contains(paymentMethod.toUpperCase());
    }
}