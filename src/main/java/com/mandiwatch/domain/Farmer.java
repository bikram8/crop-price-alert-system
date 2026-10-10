package com.mandiwatch.domain;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "farmer")
@EntityListeners(AuditingEntityListener.class)
public class Farmer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 10)
    private String phone;

    @ManyToOne(optional = false)
    @JoinColumn(name = "village_id")
    private Village village;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language = Language.ENGLISH;

    /** Filled in by Spring Data JPA auditing when the row is first inserted (see JpaConfig). */
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime registeredAt;

    @OneToMany(mappedBy = "farmer", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<PriceAlert> alerts = new ArrayList<>();

    protected Farmer() {
    }

    public Farmer(String name, String phone, Village village, Language language) {
        this.name = name;
        this.phone = phone;
        this.village = village;
        this.language = language;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public Village getVillage() { return village; }
    public Language getLanguage() { return language; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public List<PriceAlert> getAlerts() { return alerts; }

    public String getMaskedPhone() {
        return phone.substring(0, 2) + "XXXXXX" + phone.substring(8);
    }

    /** Same id-based rule as {@link PriceAlert#equals(Object)}. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Farmer other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Farmer.class.hashCode();
    }
}
