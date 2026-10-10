package com.mandiwatch.web;

import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Rupee formatting for templates with Indian digit grouping (12,34,567).
 * Used in Thymeleaf as ${@inr.fmt(value)}.
 */
@Component("inr")
public class Inr {

    private static final Locale INDIA = Locale.of("en", "IN");

    public String fmt(Number value) {
        if (value == null) {
            return "-";
        }
        return "₹" + NumberFormat.getIntegerInstance(INDIA).format(value);
    }

    /** 20.0 -> "20", 12.5 -> "12.5" */
    public String qty(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    public String plain(Number value) {
        return value == null ? "-" : NumberFormat.getIntegerInstance(INDIA).format(value);
    }
}
