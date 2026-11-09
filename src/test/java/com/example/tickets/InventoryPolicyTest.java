package com.example.tickets;
import com.example.tickets.api.ApiException;
import com.example.tickets.service.InventoryPolicy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class InventoryPolicyTest {
 @Test void allowsExactCapacityAndCustomerLimit() {
  assertDoesNotThrow(()->InventoryPolicy.validateHold(2,4,2));
 }
 @Test void rejectsQuantityAboveSix() {
  assertEquals("INVALID_QUANTITY",assertThrows(ApiException.class,()->InventoryPolicy.validateHold(7,0,20)).code);
 }
 @Test void rejectsCustomerAggregateLimit() {
  assertEquals("CUSTOMER_LIMIT",assertThrows(ApiException.class,()->InventoryPolicy.validateHold(2,5,20)).code);
 }
 @Test void rejectsInsufficientInventory() {
  assertEquals("INSUFFICIENT_SEATS",assertThrows(ApiException.class,()->InventoryPolicy.validateHold(2,0,1)).code);
 }
}
