package dev.adriangm.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.adriangm.orders.domain.CustomerReference;
import dev.adriangm.orders.domain.Order;
import dev.adriangm.orders.domain.OrderRuleViolation;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderDomainTest {
    @Test
    void missingReferenceFailureRemainsReproducible()
    {
        assertThrows(NullPointerException.class, () -> new CustomerReference(null));
    }

    @Test
    void textIsNormalizedBeforeCheckingTheLimit()
    {
        assertEquals("ORDER-1", new CustomerReference("  ORDER-1  ").value());
        assertEquals("", new CustomerReference("   ").value());
        assertEquals(100, new CustomerReference(" " + "a".repeat(100) + " ").value().length());
    }

    @Test
    void longReferenceViolatesTheContract()
    {
        var failure = assertThrows(OrderRuleViolation.class, () -> new CustomerReference("a".repeat(101)));
        assertEquals(OrderRuleViolation.Reason.REFERENCE_TOO_LONG, failure.reason());
    }

    @Test
    void quantityBoundsAreProtectedByTheAggregate()
    {
        assertEquals(1, Order.place(UUID.randomUUID(), "ORDER-1", 1).quantity().value());
        assertEquals(100, Order.place(UUID.randomUUID(), "ORDER-1", 100).quantity().value());
        for (int quantity : new int[]{0, -1, 101}) {
            assertEquals(
                OrderRuleViolation.Reason.INVALID_QUANTITY,
                assertThrows(OrderRuleViolation.class, () -> Order.place(UUID.randomUUID(), "ORDER-1", quantity))
                    .reason()
            );
        }
    }
}
