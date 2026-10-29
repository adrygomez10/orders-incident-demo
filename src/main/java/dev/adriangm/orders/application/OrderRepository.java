package dev.adriangm.orders.application;

import dev.adriangm.orders.domain.Order;

/** Persists valid order aggregates without exposing SQL or framework-specific types. */
public interface OrderRepository {
    /**
     * Persists the supplied valid aggregate or reports failure without inventing a successful result.
     */
    void save(Order order);
}
