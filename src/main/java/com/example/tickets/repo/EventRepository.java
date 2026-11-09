package com.example.tickets.repo;
import com.example.tickets.domain.Event;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface EventRepository extends JpaRepository<Event,UUID> {
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select e from Event e where e.id = :id")
 Optional<Event> lockById(@Param("id") UUID id);
}
