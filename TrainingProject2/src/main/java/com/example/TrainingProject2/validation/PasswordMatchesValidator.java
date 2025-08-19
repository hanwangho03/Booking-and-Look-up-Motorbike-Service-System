package com.example.TrainingProject2.validation;

import com.example.TrainingProject2.dto.auth.RegisterRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

    @Override
    public void initialize(PasswordMatches constraintAnnotation) {}

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext context) {
        RegisterRequestDto user = (RegisterRequestDto) obj;
        boolean isValid = user.getPassword().equals(user.getConfirmPassword());
        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("confirmPassword").addConstraintViolation();
        }
        return isValid;
    }
}