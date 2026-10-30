package com.doch.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.StringUtils;

public class OneOfValidator implements ConstraintValidator<OneOf, Object> {

    private String[] fields;

    @Override
    public void initialize(OneOf constraintAnnotation) {
        this.fields = constraintAnnotation.fields();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        BeanWrapperImpl beanWrapper = new BeanWrapperImpl(value);
        int presentCount = 0;

        for (String fieldName : fields) {
            Object fieldValue = beanWrapper.getPropertyValue(fieldName);
            if (fieldValue != null && StringUtils.hasText(fieldValue.toString())) {
                presentCount++;
            }
        }

        // The validation is successful if EXACTLY one of the fields is present.
        return presentCount == 1;
    }
}