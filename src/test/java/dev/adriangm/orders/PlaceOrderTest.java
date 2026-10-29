package dev.adriangm.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import dev.adriangm.orders.application.PlaceOrder;
import dev.adriangm.orders.application.PlaceOrderUseCase;
import dev.adriangm.orders.domain.Order;
import dev.adriangm.orders.domain.OrderRuleViolation;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlaceOrderTest {
    @Test
    void persistsValidatedOrderBeforeReturningResult()
    {
        var saved = new ArrayList<Order>();
        UUID id = UUID.randomUUID();
        var useCase = new PlaceOrder(saved::add, () -> id);
        var result = useCase.execute(new PlaceOrderUseCase.Command(" ORDER-1 ", 2));
        assertEquals(id, result.id());
        assertEquals("ORDER-1", result.customerReference());
        assertEquals(1, saved.size());
        assertEquals(2, saved.getFirst().quantity().value());
    }

    @Test
    void invalidOrderNeverReachesPersistence()
    {
        var useCase = new PlaceOrder(order -> fail("Invalid aggregate reached storage"), UUID::randomUUID);
        assertThrows(OrderRuleViolation.class, () -> useCase.execute(new PlaceOrderUseCase.Command("ORDER-1", 0)));
    }
}
