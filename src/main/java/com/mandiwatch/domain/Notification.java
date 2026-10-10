package com.mandiwatch.domain;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/** A message we sent (or tried to send) to a farmer. Doubles as the outbox log. */
@Entity
@Table(name = "notification")
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "farmer_id")
    private Farmer farmer;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private String channel;

    private boolean delivered;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    protected Notification() {
    }

    public Notification(Farmer farmer, String message, String channel, boolean delivered) {
        this.farmer = farmer;
        this.message = message;
        this.channel = channel;
        this.delivered = delivered;
    }

    public Long getId() { return id; }
    public Farmer getFarmer() { return farmer; }
    public String getMessage() { return message; }
    public String getChannel() { return channel; }
    public boolean isDelivered() { return delivered; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
