// src/main/java/com/example/Trainning/Project/validation/PasswordMatchesValidator.java
package com.example.Trainning.Project.validation;

import com.example.Trainning.Project.dto.Customer.CustomerRegistrationRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

    @Override
    public void initialize(PasswordMatches constraintAnnotation) {
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        CustomerRegistrationRequest user = (CustomerRegistrationRequest) obj;
        boolean isValid = user.getPassword().equals(user.getConfirmPassword());

        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("confirmPassword")
                    .addConstraintViolation();
        }
        return isValid;
    }
}