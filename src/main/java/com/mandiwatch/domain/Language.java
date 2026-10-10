package com.mandiwatch.domain;

public enum Language {
    ENGLISH("English"),
    TELUGU("తెలుగు"),
    HINDI("हिन्दी");

    private final String label;

    Language(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
