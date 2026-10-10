package com.mandiwatch.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One price row as it arrives, before cleaning. Both the API client and the
 * CSV parser turn their input into these, so the import logic only has to
 * deal with one shape.
 */
public record RawPriceRow(String state,
                          String district,
                          String market,
                          String commodity,
                          String variety,
                          String arrivalDate,
                          String minPrice,
                          String maxPrice,
                          String modalPrice) {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd-MM-yyyy"));

    /**
     * Builds a row from a key/value map (a CSV line or a JSON record).
     * Header names differ between the API ("modal_price"), the CSV download
     * ("Modal_x0020_Price") and hand-made files ("Modal Price"), so keys are
     * normalised first.
     */
    public static RawPriceRow fromMap(Map<String, ?> source) {
        Map<String, String> m = new HashMap<>();
        source.forEach((k, v) -> m.put(normaliseKey(k), v == null ? null : String.valueOf(v).trim()));
        return new RawPriceRow(
                m.get("state"),
                m.get("district"),
                m.get("market"),
                m.get("commodity"),
                m.get("variety"),
                firstNonNull(m.get("arrival_date"), m.get("price_date"), m.get("date")),
                m.get("min_price"),
                m.get("max_price"),
                m.get("modal_price"));
    }

    private static String firstNonNull(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    static String normaliseKey(String key) {
        return key.trim()
                .toLowerCase(Locale.ROOT)
                .replace("_x0020_", "_")
                .replace(' ', '_')
                .replace("(rs./quintal)", "")
                .replaceAll("_+$", "");
    }

    public LocalDate parseDate() {
        if (arrivalDate == null || arrivalDate.isBlank()) {
            return null;
        }
        String value = arrivalDate.length() > 10 ? arrivalDate.substring(0, 10) : arrivalDate;
        for (DateTimeFormatter f : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, f);
            } catch (DateTimeParseException ignored) {
                // try the next format
            }
        }
        return null;
    }

    public static Integer parsePrice(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return (int) Math.round(Double.parseDouble(value.replace(",", "")));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
