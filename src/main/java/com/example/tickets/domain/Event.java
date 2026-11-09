package com.example.tickets.domain;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="events")
public class Event {
 @Id public UUID id;
 @Column(nullable=false) public String name;
 @Column(name="starts_at",nullable=false) public Instant startsAt;
 @Column(name="total_seats",nullable=false) public int totalSeats;
 @Column(name="available_seats",nullable=false) public int availableSeats;
 @Column(name="held_seats",nullable=false) public int heldSeats;
 @Column(name="booked_seats",nullable=false) public int bookedSeats;
 protected Event() {}
 public Event(String name, Instant startsAt, int seats) {
  this.id=UUID.randomUUID(); this.name=name; this.startsAt=startsAt;
  this.totalSeats=seats; this.availableSeats=seats;
 }
}
