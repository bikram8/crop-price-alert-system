package com.mandiwatch.web;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom class-level (cross-field) constraint: latitude and longitude must be given together.
 * A field-level annotation can't express this because it only ever sees one field.
 * The logic lives in {@link BothCoordinatesOrNoneValidator}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BothCoordinatesOrNoneValidator.class)
public @interface BothCoordinatesOrNone {

    String message() default "Enter both latitude and longitude, or leave both empty.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
