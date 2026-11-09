package com.example.tickets.service;
import com.example.tickets.api.ApiException;
import org.springframework.http.HttpStatus;
/** Pure business rules, independently unit-testable. */
public final class InventoryPolicy {
 private InventoryPolicy() {}
 public static void validateHold(int requested, int alreadyHeld, int available) {
  if (requested < 1 || requested > 6)
   throw new ApiException(HttpStatus.BAD_REQUEST,"INVALID_QUANTITY","Quantity must be between 1 and 6");
  if (alreadyHeld + requested > 6)
   throw new ApiException(HttpStatus.CONFLICT,"CUSTOMER_LIMIT","Maximum 6 active seats per customer per event");
  if (available < requested)
   throw new ApiException(HttpStatus.CONFLICT,"INSUFFICIENT_SEATS","Not enough available seats");
 }
}
