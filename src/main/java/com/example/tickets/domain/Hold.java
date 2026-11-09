package com.example.tickets.domain;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="holds")
public class Hold {
 @Id public UUID id;
 @Column(name="event_id",nullable=false) public UUID eventId;
 @Column(name="customer_id",nullable=false) public UUID customerId;
 @Column(nullable=false) public int quantity;
 @Enumerated(EnumType.STRING) @Column(nullable=false) public HoldStatus status;
 @Column(name="expires_at",nullable=false) public Instant expiresAt;
 @Column(name="created_at",nullable=false) public Instant createdAt;
 protected Hold() {}
 public Hold(UUID eventId, UUID customerId, int quantity, Instant now, Instant expiresAt) {
  this.id=UUID.randomUUID(); this.eventId=eventId; this.customerId=customerId;
  this.quantity=quantity; this.createdAt=now; this.expiresAt=expiresAt; this.status=HoldStatus.ACTIVE;
 }
}
