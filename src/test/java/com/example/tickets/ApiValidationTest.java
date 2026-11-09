package com.example.tickets;
import com.example.tickets.api.ApiDtos.*;
import jakarta.validation.*;
import org.junit.jupiter.api.*;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class ApiValidationTest {
 private final Validator validator=Validation.buildDefaultValidatorFactory().getValidator();
 @Test void eventMustHavePositiveCapacityAndFutureDate() {
  assertFalse(validator.validate(new CreateEvent("",Instant.now().minusSeconds(10),0)).isEmpty());
  assertTrue(validator.validate(new CreateEvent("Concert",Instant.now().plusSeconds(3600),10)).isEmpty());
 }
 @Test void holdQuantityMustBeBetweenOneAndSix() {
  assertFalse(validator.validate(new CreateHold(UUID.randomUUID(),7)).isEmpty());
  assertFalse(validator.validate(new CreateHold(UUID.randomUUID(),0)).isEmpty());
  assertTrue(validator.validate(new CreateHold(UUID.randomUUID(),6)).isEmpty());
 }
}
