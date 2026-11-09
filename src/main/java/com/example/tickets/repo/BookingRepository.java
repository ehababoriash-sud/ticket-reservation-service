package com.example.tickets.repo;
import com.example.tickets.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface BookingRepository extends JpaRepository<Booking,UUID> {
 Optional<Booking> findByHoldId(UUID holdId);
 List<Booking> findByCustomerIdOrderByConfirmedAtDesc(UUID customerId);
}
