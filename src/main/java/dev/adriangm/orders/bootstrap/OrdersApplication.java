package dev.adriangm.orders.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Spring Boot entry point for the independent order application. */
@SpringBootApplication(scanBasePackages = "dev.adriangm.orders")
public class OrdersApplication {
    /** Starts the configured application and its framework-managed lifecycle. */
    public static void main(String[] args)
    {
        SpringApplication.run(OrdersApplication.class, args);
    }
}
