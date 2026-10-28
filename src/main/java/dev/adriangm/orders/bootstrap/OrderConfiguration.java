package dev.adriangm.orders.bootstrap;

import dev.adriangm.orders.adapter.out.persistence.JdbcOrderRepository;
import dev.adriangm.orders.adapter.out.telemetry.SentryOrderTelemetry;
import dev.adriangm.orders.application.OrderRepository;
import dev.adriangm.orders.application.PlaceOrder;
import dev.adriangm.orders.application.PlaceOrderUseCase;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Composes order placement, persistence and optional Sentry observation at the framework boundary.
 */
@Configuration
public class OrderConfiguration {
  /** Connects aggregate persistence to its JDBC adapter. */
  @Bean
  public OrderRepository orders(JdbcTemplate jdbc) {
    return new JdbcOrderRepository(jdbc);
  }

  /** Connects placement with repository access and generation of unique order identities. */
  @Bean
  public PlaceOrderUseCase placeOrder(OrderRepository orders) {
    return new PlaceOrder(orders, UUID::randomUUID);
  }

  /** Configures optional Sentry capture and closes it with the application lifecycle. */
  @Bean(destroyMethod = "close")
  public SentryOrderTelemetry orderTelemetry(Environment environment) {
    return new SentryOrderTelemetry(
        environment.getProperty("SENTRY_DSN", ""),
        environment.getProperty("APP_RELEASE", "orders-demo@local"),
        environment.getProperty("DEMO_RUN", "manual"));
  }
}
