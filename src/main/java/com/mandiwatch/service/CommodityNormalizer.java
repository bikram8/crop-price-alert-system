package com.mandiwatch.service;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The government feed spells the same crop in many ways: "Paddy(Dhan)(Common)",
 * "PADDY", "Dhan", "Tomato " with a trailing space, and so on. If we stored them
 * as-is, the same crop would show up as five different commodities and price
 * comparisons would be useless. This class maps known spellings to one name.
 */
@Component
public class CommodityNormalizer {

    private final Map<String, String> aliases = new HashMap<>();

    public CommodityNormalizer() {
        alias("Tomato", "tomato", "tamatar", "tomatoes");
        alias("Onion", "onion", "pyaz", "onion green");
        alias("Potato", "potato", "aloo");
        alias("Green Chilli", "green chilli", "green chilly", "chilly green", "chillies green", "green chillies");
        alias("Dry Chillies", "dry chillies", "red chilli", "chilli red", "dry chilli", "chillies dry");
        alias("Brinjal", "brinjal", "baingan", "eggplant");
        alias("Cotton", "cotton", "kapas", "cotton (kapas)");
        alias("Paddy", "paddy", "paddy(dhan)", "paddy(dhan)(common)", "paddy(dhan)(basmati)", "dhan", "paddy dhan");
        alias("Maize", "maize", "corn", "makka");
        alias("Turmeric", "turmeric", "haldi", "turmeric (raw)");
        alias("Groundnut", "groundnut", "groundnut pods (raw)", "moong fali");
        alias("Bhindi", "bhindi", "bhindi(ladies finger)", "ladies finger", "okra");
    }

    private void alias(String canonical, String... spellings) {
        for (String s : spellings) {
            aliases.put(key(s), canonical);
        }
    }

    public String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String known = aliases.get(key(raw));
        if (known != null) {
            return known;
        }
        // Unknown crop: keep it, just tidy the case so "BEANS" and "Beans" end up the same.
        return titleCase(raw.trim().replaceAll("\\s+", " "));
    }

    private static String key(String s) {
        return s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static String titleCase(String s) {
        StringBuilder out = new StringBuilder(s.length());
        boolean startOfWord = true;
        for (char ch : s.toCharArray()) {
            if (Character.isLetter(ch)) {
                out.append(startOfWord ? Character.toUpperCase(ch) : Character.toLowerCase(ch));
                startOfWord = false;
            } else {
                out.append(ch);
                startOfWord = true;
            }
        }
        return out.toString();
    }
}
