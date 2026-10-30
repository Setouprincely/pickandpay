package com.doch.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PaymentMethodValidator.class)
public @interface ValidPaymentMethod {
    String message() default "Invalid payment method. Allowed values are MTN_MOMO, ORANGE_MONEY";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}