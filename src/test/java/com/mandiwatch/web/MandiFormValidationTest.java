package com.mandiwatch.web;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Bean Validation on its own (no Spring): the field rules plus the custom @BothCoordinatesOrNone. */
class MandiFormValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @ParameterizedTest(name = "lat={0}, lon={1}, fee={2} -> valid={3}")
    @CsvSource(nullValues = "null", value = {
            "17.47, 78.47, 1.0, true",   // normal
            "null,  null,  1.0, true",   // no location yet is allowed
            "17.47, null,  1.0, false",  // only one coordinate: custom class-level rule
            "null,  78.47, 1.0, false",
            "51.50, -0.12, 1.0, false",  // London, outside India
            "17.47, 78.47, 12,  false",  // fee too high
            "17.47, 78.47, null, false", // fee missing
    })
    void validatesMandiForm(Double lat, Double lon, Double fee, boolean expectedValid) {
        MandiForm form = new MandiForm();
        form.setLatitude(lat);
        form.setLongitude(lon);
        form.setFeePercent(fee);

        Set<ConstraintViolation<MandiForm>> violations = validator.validate(form);

        assertThat(violations.isEmpty()).as(violations.toString()).isEqualTo(expectedValid);
    }
}
