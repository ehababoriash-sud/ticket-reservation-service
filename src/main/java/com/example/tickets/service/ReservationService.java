package com.example.tickets.service;
import com.example.tickets.api.*;
import com.example.tickets.domain.*;
import com.example.tickets.repo.*;
import org.springframework.beans.factory.annotation.Value;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service
public class ReservationService {
 private final EventRepository events;
 private final HoldRepository holds;
 private final BookingRepository bookings;
 private final Clock clock;
 private final EntityManager entityManager;
 private final Duration holdDuration;
 public ReservationService(EventRepository events, HoldRepository holds, BookingRepository bookings,
   Clock clock, EntityManager entityManager, @Value("${app.hold-duration}") Duration holdDuration) {
  this.events=events; this.holds=holds; this.bookings=bookings;
  this.clock=clock; this.entityManager=entityManager; this.holdDuration=holdDuration;
 }
 @Transactional
 public Event createEvent(String name, Instant startsAt, int seats) {
  return events.save(new Event(name,startsAt,seats));
 }
 @Transactional
 public Event getEvent(UUID id) {
  Event e=lockEvent(id);
  expireForEvent(e,clock.instant());
  return e;
 }
 @Transactional
 public Hold createHold(UUID eventId, UUID customerId, int quantity) {
  Event e=lockEvent(eventId);
  Instant now=clock.instant();
  expireForEvent(e,now);
  int customerHeld=holds.findByEventIdAndCustomerIdAndStatus(eventId,customerId,HoldStatus.ACTIVE)
    .stream().mapToInt(h->h.quantity).sum();
  InventoryPolicy.validateHold(quantity,customerHeld,e.availableSeats);
  Hold h=new Hold(eventId,customerId,quantity,now,now.plus(holdDuration));
  e.availableSeats-=quantity; e.heldSeats+=quantity;
  return holds.save(h);
 }
 @Transactional
 public Booking confirm(UUID holdId) {
  Hold snapshot=holds.findById(holdId).orElseThrow(()->missing("Hold not found"));
  Event e=lockEvent(snapshot.eventId);
  Hold h=holds.findById(holdId).orElseThrow(()->missing("Hold not found"));
  entityManager.refresh(h); // Snapshot may be stale after waiting for the event lock.
  Instant now=clock.instant();
  expireForEvent(e,now);
  if (h.status==HoldStatus.CONFIRMED) return bookings.findByHoldId(holdId)
    .orElseThrow(()->new IllegalStateException("Confirmed hold without booking"));
  if (h.status==HoldStatus.EXPIRED || !h.expiresAt.isAfter(now))
    throw new ApiException(HttpStatus.GONE,"HOLD_EXPIRED","Hold has expired");
  if (h.status!=HoldStatus.ACTIVE) throw conflict("HOLD_NOT_ACTIVE","Hold cannot be confirmed");
  h.status=HoldStatus.CONFIRMED;
  e.heldSeats-=h.quantity; e.bookedSeats+=h.quantity;
  return bookings.save(new Booking(h,now));
 }
 @Transactional
 public void cancel(UUID holdId) {
  Hold snapshot=holds.findById(holdId).orElseThrow(()->missing("Hold not found"));
  Event e=lockEvent(snapshot.eventId);
  Hold h=holds.findById(holdId).orElseThrow(()->missing("Hold not found"));
  entityManager.refresh(h); // Re-read committed status after acquiring the event lock.
  expireForEvent(e,clock.instant());
  if (h.status==HoldStatus.CANCELLED || h.status==HoldStatus.EXPIRED) return;
  if (h.status==HoldStatus.CONFIRMED) throw conflict("ALREADY_BOOKED","Confirmed booking cannot be cancelled here");
  h.status=HoldStatus.CANCELLED;
  e.heldSeats-=h.quantity; e.availableSeats+=h.quantity;
 }
 @Transactional(readOnly=true)
 public List<Booking> bookingsFor(UUID customerId) {
  return bookings.findByCustomerIdOrderByConfirmedAtDesc(customerId);
 }
 @Transactional
 public void expireEvent(UUID eventId) {
  Event e=lockEvent(eventId);
  expireForEvent(e,clock.instant());
 }
 private Event lockEvent(UUID id) { return events.lockById(id).orElseThrow(()->missing("Event not found")); }
 private void expireForEvent(Event e, Instant now) {
  for (Hold h:holds.findByEventIdAndStatusAndExpiresAtLessThanEqual(e.id,HoldStatus.ACTIVE,now)) {
   h.status=HoldStatus.EXPIRED;
   e.heldSeats-=h.quantity;
   e.availableSeats+=h.quantity;
  }
 }
 private static ApiException missing(String message) { return new ApiException(HttpStatus.NOT_FOUND,"NOT_FOUND",message); }
 private static ApiException conflict(String code,String message) { return new ApiException(HttpStatus.CONFLICT,code,message); }
}
