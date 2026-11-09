package com.example.tickets.api;
import com.example.tickets.domain.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;
public final class ApiDtos {
 private ApiDtos() {}
 public record CreateEvent(@NotBlank String name, @NotNull @Future Instant startsAt, @Min(1) int totalSeats) {}
 public record CreateHold(@NotNull UUID customerId, @Min(1) @Max(6) int quantity) {}
 public record EventView(UUID id, String name, Instant startsAt, int totalSeats, int available, int held, int booked) {
  public static EventView from(Event e) { return new EventView(e.id,e.name,e.startsAt,e.totalSeats,e.availableSeats,e.heldSeats,e.bookedSeats); }
 }
 public record HoldView(UUID id, UUID eventId, UUID customerId, int quantity, String status, Instant expiresAt) {
  public static HoldView from(Hold h) { return new HoldView(h.id,h.eventId,h.customerId,h.quantity,h.status.name(),h.expiresAt); }
 }
 public record BookingView(UUID id, UUID holdId, UUID eventId, UUID customerId, int quantity, Instant confirmedAt) {
  public static BookingView from(Booking b) { return new BookingView(b.id,b.holdId,b.eventId,b.customerId,b.quantity,b.confirmedAt); }
 }
}
