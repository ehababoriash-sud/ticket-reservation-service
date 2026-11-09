package com.example.tickets;
import com.example.tickets.api.ApiException;
import com.example.tickets.domain.*;
import com.example.tickets.repo.*;
import com.example.tickets.service.ReservationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @Testcontainers
class ReservationIntegrationTest {
 @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
   .withDatabaseName("tickets").withUsername("tickets").withPassword("tickets");
 @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
  registry.add("spring.datasource.url",postgres::getJdbcUrl);
  registry.add("spring.datasource.username",postgres::getUsername);
  registry.add("spring.datasource.password",postgres::getPassword);
  registry.add("app.expiry-sweep-ms",()->"3600000");
 }
 @Autowired ReservationService service;
 @Autowired EventRepository events;
 @Autowired HoldRepository holds;
 @Autowired BookingRepository bookings;
 private Event event(int seats) { return service.createEvent("Concert",Instant.now().plus(2,ChronoUnit.DAYS),seats); }
 @Test void holdConfirmAndIdempotency() {
  Event e=event(10); UUID customer=UUID.randomUUID();
  Hold h=service.createHold(e.id,customer,3);
  Booking first=service.confirm(h.id);
  Booking second=service.confirm(h.id);
  assertEquals(first.id,second.id);
  assertEquals(1,bookings.findByCustomerIdOrderByConfirmedAtDesc(customer).size());
  Event current=service.getEvent(e.id);
  assertEquals(7,current.availableSeats); assertEquals(0,current.heldSeats); assertEquals(3,current.bookedSeats);
 }
 @Test void rejectsExcessiveCustomerHoldsAndInsufficientSeats() {
  Event e=event(8); UUID customer=UUID.randomUUID();
  service.createHold(e.id,customer,5);
  ApiException limit=assertThrows(ApiException.class,()->service.createHold(e.id,customer,2));
  assertEquals(HttpStatus.CONFLICT,limit.status);
  ApiException unavailable=assertThrows(ApiException.class,()->service.createHold(e.id,UUID.randomUUID(),4));
  assertEquals("INSUFFICIENT_SEATS",unavailable.code);
 }
 @Test void cancellationReturnsInventory() {
  Event e=event(2); Hold h=service.createHold(e.id,UUID.randomUUID(),2);
  service.cancel(h.id); service.cancel(h.id);
  assertEquals(2,service.getEvent(e.id).availableSeats);
 }
 @Test void expiredHoldIsRejectedAndReleased() {
  Event e=event(1); Hold h=service.createHold(e.id,UUID.randomUUID(),1);
  // Directly adjust expiry to avoid sleeps and prove expiry behavior against PostgreSQL.
  h=holds.findById(h.id).orElseThrow(); h.expiresAt=Instant.now().minusSeconds(1); holds.saveAndFlush(h);
  UUID holdId=h.id;
  ApiException gone=assertThrows(ApiException.class,()->service.confirm(holdId));
  assertEquals(HttpStatus.GONE,gone.status);
  service.expireEvent(e.id);
  assertEquals(1,service.getEvent(e.id).availableSeats);
 }
 @Test void concurrentCustomersNeverOversell() throws Exception {
  Event e=event(10); int attempts=200;
  ExecutorService pool=Executors.newFixedThreadPool(32);
  CountDownLatch ready=new CountDownLatch(32), go=new CountDownLatch(1);
  List<Future<Boolean>> results=new ArrayList<>();
  try {
   for(int i=0;i<attempts;i++) results.add(pool.submit(()->{
    ready.countDown(); go.await();
    try { service.createHold(e.id,UUID.randomUUID(),1); return true; }
    catch(ApiException ex) { if(ex.status!=HttpStatus.CONFLICT) throw ex; return false; }
   }));
   assertTrue(ready.await(10,TimeUnit.SECONDS)); go.countDown();
   int successes=0;
   for(Future<Boolean> result:results) if(result.get(60,TimeUnit.SECONDS)) successes++;
   assertEquals(10,successes);
   Event current=service.getEvent(e.id);
   assertEquals(0,current.availableSeats); assertEquals(10,current.heldSeats);
   assertEquals(current.totalSeats,current.availableSeats+current.heldSeats+current.bookedSeats);
  } finally { go.countDown(); pool.shutdownNow(); }
 }
 @Test void concurrentConfirmIsIdempotent() throws Exception {
  Event e=event(1); Hold h=service.createHold(e.id,UUID.randomUUID(),1);
  ExecutorService pool=Executors.newFixedThreadPool(2);
  CountDownLatch go=new CountDownLatch(1);
  try {
   Future<UUID> a=pool.submit(()->{go.await();return service.confirm(h.id).id;});
   Future<UUID> b=pool.submit(()->{go.await();return service.confirm(h.id).id;});
   go.countDown(); assertEquals(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS));
   assertEquals(1,service.getEvent(e.id).bookedSeats);
  } finally {go.countDown();pool.shutdownNow();}
 }
}
