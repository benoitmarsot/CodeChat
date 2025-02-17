package com.unbumpkin.codechat.exception;

import jakarta.validation.ConstraintViolation;
import java.util.Set;
import java.util.stream.Collectors;

public class ValidationException extends RuntimeException {
    public ValidationException(Set<? extends ConstraintViolation<?>> violations) {
        super(violations.stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .collect(Collectors.joining(", ")));
    }
}