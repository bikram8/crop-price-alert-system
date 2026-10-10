package com.mandiwatch.web;

import com.mandiwatch.domain.AlertCondition;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class AlertForm {

    @NotNull(message = "Pick a crop")
    private Long commodityId;

    @NotNull
    private AlertCondition condition = AlertCondition.ABOVE;

    @Min(value = 100, message = "Price should be at least ₹100/qtl")
    @Max(value = 200000, message = "That price looks too high")
    private int threshold = 2500;

    @Min(value = 5, message = "Radius must be at least 5 km")
    @Max(value = 500, message = "Radius can be at most 500 km")
    private int radiusKm = 80;

    public Long getCommodityId() { return commodityId; }
    public void setCommodityId(Long commodityId) { this.commodityId = commodityId; }
    public AlertCondition getCondition() { return condition; }
    public void setCondition(AlertCondition condition) { this.condition = condition; }
    public int getThreshold() { return threshold; }
    public void setThreshold(int threshold) { this.threshold = threshold; }
    public int getRadiusKm() { return radiusKm; }
    public void setRadiusKm(int radiusKm) { this.radiusKm = radiusKm; }
}
