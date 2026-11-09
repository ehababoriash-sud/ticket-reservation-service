package com.example.tickets.service;
import com.example.tickets.repo.HoldRepository;
import com.example.tickets.domain.HoldStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Clock;
@Component
public class ExpiryScheduler {
 private final HoldRepository holds;
 private final ReservationService service;
 private final Clock clock;
 public ExpiryScheduler(HoldRepository holds, ReservationService service, Clock clock) {
  this.holds=holds; this.service=service; this.clock=clock;
 }
 @Scheduled(fixedDelayString="${app.expiry-sweep-ms}")
 public void sweep() {
  // Each event is locked in its own transaction; duplicate workers are safe.
  for (var eventId:holds.findEventsWithExpiredHolds(HoldStatus.ACTIVE,clock.instant())) service.expireEvent(eventId);
 }
}
