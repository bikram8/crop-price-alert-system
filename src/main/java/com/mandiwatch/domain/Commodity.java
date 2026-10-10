package com.mandiwatch.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "commodity")
public class Commodity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    /** Local (Telugu) name shown next to the English one, e.g. "టమాటా". */
    private String localName;

    protected Commodity() {
    }

    public Commodity(String name, String localName) {
        this.name = name;
        this.localName = localName;
    }

    public String getDisplayName() {
        return localName == null || localName.isBlank() ? name : name + " (" + localName + ")";
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getLocalName() { return localName; }

    /** Same id-based rule as {@link PriceAlert#equals(Object)}. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Commodity other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Commodity.class.hashCode();
    }
}
