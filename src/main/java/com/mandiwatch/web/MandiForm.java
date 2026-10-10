package com.mandiwatch.web;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Replaces the hand-written if-checks that used to sit in MandiController.update().
 * The bounds are a rough box around India.
 */
@BothCoordinatesOrNone
public class MandiForm {

    @DecimalMin(value = "6", message = "Latitude must be inside India (6 to 37)")
    @DecimalMax(value = "37", message = "Latitude must be inside India (6 to 37)")
    private Double latitude;

    @DecimalMin(value = "68", message = "Longitude must be inside India (68 to 98)")
    @DecimalMax(value = "98", message = "Longitude must be inside India (68 to 98)")
    private Double longitude;

    @NotNull(message = "Fee is required")
    @DecimalMin(value = "0", message = "Fee should be between 0 and 10%")
    @DecimalMax(value = "10", message = "Fee should be between 0 and 10%")
    private Double feePercent;

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getFeePercent() { return feePercent; }
    public void setFeePercent(Double feePercent) { this.feePercent = feePercent; }
}
