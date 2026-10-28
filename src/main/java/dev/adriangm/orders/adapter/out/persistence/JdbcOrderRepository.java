package dev.adriangm.orders.adapter.out.persistence;

import dev.adriangm.orders.application.OrderRepository;
import dev.adriangm.orders.domain.Order;
import org.springframework.jdbc.core.JdbcTemplate;

/** Translates validated order aggregates to parameterized PostgreSQL inserts. */
public final class JdbcOrderRepository implements OrderRepository {
  private final JdbcTemplate jdbc;

  /** Receives JDBC access at the persistence boundary. */
  public JdbcOrderRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void save(Order order) {
    jdbc.update(
        "INSERT INTO orders(id,customer_reference,quantity) VALUES (?,?,?)",
        order.id(),
        order.customerReference().value(),
        order.quantity().value());
  }
}
