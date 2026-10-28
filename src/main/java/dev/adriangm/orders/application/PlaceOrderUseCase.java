package dev.adriangm.orders.application;

import java.util.UUID;

/** Places an order without requiring an HTTP controller or a particular persistence technology. */
public interface PlaceOrderUseCase {
  /** Application order input before the aggregate validates reference and quantity. */
  record Command(String customerReference, int quantity) {}

  /** Identifier and normalized reference of the order successfully saved by the use case. */
  record Result(UUID id, String customerReference) {}

  /**
   * Creates and saves a valid order before returning its persisted identity and normalized
   * reference.
   */
  Result execute(Command command);
}
