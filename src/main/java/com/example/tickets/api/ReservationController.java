package com.example.tickets.api;
import com.example.tickets.service.ReservationService;
import com.example.tickets.api.ApiDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
public class ReservationController {
 private final ReservationService service;
 public ReservationController(ReservationService service) { this.service=service; }
 @PostMapping("/events")
 public ResponseEntity<EventView> create(@Valid @RequestBody CreateEvent req) {
  EventView result=EventView.from(service.createEvent(req.name(),req.startsAt(),req.totalSeats()));
  return ResponseEntity.status(HttpStatus.CREATED).body(result);
 }
 @GetMapping("/events/{id}")
 public EventView event(@PathVariable UUID id) { return EventView.from(service.getEvent(id)); }
 @PostMapping("/events/{id}/holds")
 public ResponseEntity<HoldView> hold(@PathVariable UUID id,@Valid @RequestBody CreateHold req) {
  return ResponseEntity.status(HttpStatus.CREATED).body(HoldView.from(service.createHold(id,req.customerId(),req.quantity())));
 }
 @PostMapping("/holds/{id}/confirm")
 public BookingView confirm(@PathVariable UUID id) { return BookingView.from(service.confirm(id)); }
 @DeleteMapping("/holds/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void cancel(@PathVariable UUID id) { service.cancel(id); }
 @GetMapping("/customers/{id}/bookings")
 public List<BookingView> bookings(@PathVariable UUID id) {
  return service.bookingsFor(id).stream().map(BookingView::from).toList();
 }
}
