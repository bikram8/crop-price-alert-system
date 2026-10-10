package com.mandiwatch.domain;

public enum AlertCondition {
    /** Fire when the modal price in some nearby mandi goes at or above the threshold. */
    ABOVE("goes above"),
    /** Fire when the modal price goes at or below the threshold (useful for buyers / input planning). */
    BELOW("drops below");

    private final String phrase;

    AlertCondition(String phrase) {
        this.phrase = phrase;
    }

    public String getPhrase() {
        return phrase;
    }

    public boolean isMet(int price, int threshold) {
        return this == ABOVE ? price >= threshold : price <= threshold;
    }
}
