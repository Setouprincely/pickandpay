package com.doch.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE}) // This annotation applies to the class level
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = OneOfValidator.class)
public @interface OneOf {
    String message() default "Either orderId or xenderRequestId must be provided, but not both.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String[] fields(); // We will specify the field names here
}