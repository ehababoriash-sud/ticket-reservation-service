package com.example.tickets.repo;
import com.example.tickets.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;
public interface HoldRepository extends JpaRepository<Hold,UUID> {
 List<Hold> findByEventIdAndStatusAndExpiresAtLessThanEqual(UUID eventId, HoldStatus status, Instant cutoff);
 List<Hold> findByEventIdAndCustomerIdAndStatus(UUID eventId, UUID customerId, HoldStatus status);
 @Query("select distinct h.eventId from Hold h where h.status = :status and h.expiresAt <= :now")
 List<UUID> findEventsWithExpiredHolds(@Param("status") HoldStatus status, @Param("now") Instant now);
}
