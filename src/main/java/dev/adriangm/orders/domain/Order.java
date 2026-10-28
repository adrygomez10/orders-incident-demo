package dev.adriangm.orders.domain;

import java.util.Objects;
import java.util.UUID;

/** Immutable order aggregate created only from a valid reference and quantity. */
public final class Order {
  private final UUID id;
  private final CustomerReference customerReference;
  private final OrderQuantity quantity;

  private Order(UUID id, CustomerReference reference, OrderQuantity quantity) {
    this.id = Objects.requireNonNull(id);
    this.customerReference = Objects.requireNonNull(reference);
    this.quantity = Objects.requireNonNull(quantity);
  }

  /** Creates an order only after its reference and quantity satisfy the business rules. */
  public static Order place(UUID id, String reference, int quantity) {
    return new Order(id, new CustomerReference(reference), new OrderQuantity(quantity));
  }

  /** Returns the immutable order identity. */
  public UUID id() {
    return id;
  }

  /** Returns the normalized optional reference as a value object. */
  public CustomerReference customerReference() {
    return customerReference;
  }

  /** Returns the validated quantity as a value object. */
  public OrderQuantity quantity() {
    return quantity;
  }
}
