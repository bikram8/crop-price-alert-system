package com.mandiwatch.web;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BothCoordinatesOrNoneValidator implements ConstraintValidator<BothCoordinatesOrNone, MandiForm> {

    @Override
    public boolean isValid(MandiForm form, ConstraintValidatorContext context) {
        if (form == null) {
            return true; // null-handling is @NotNull's job, by Bean Validation convention
        }
        return (form.getLatitude() == null) == (form.getLongitude() == null);
    }
}
