package com.example.tickets.domain;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="bookings")
public class Booking {
 @Id public UUID id;
 @Column(name="hold_id",nullable=false,unique=true) public UUID holdId;
 @Column(name="event_id",nullable=false) public UUID eventId;
 @Column(name="customer_id",nullable=false) public UUID customerId;
 @Column(nullable=false) public int quantity;
 @Column(name="confirmed_at",nullable=false) public Instant confirmedAt;
 protected Booking() {}
 public Booking(Hold hold, Instant now) {
  id=UUID.randomUUID(); holdId=hold.id; eventId=hold.eventId;
  customerId=hold.customerId; quantity=hold.quantity; confirmedAt=now;
 }
}
