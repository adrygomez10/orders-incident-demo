package dev.adriangm.orders.application;

import dev.adriangm.orders.domain.Order;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** Creates a valid order aggregate, persists it through a port and returns placement data. */
public final class PlaceOrder implements PlaceOrderUseCase {
    private final OrderRepository repository;
    private final Supplier<UUID> identifiers;

    /** Receives aggregate persistence and replaceable generation of order identities. */
    public PlaceOrder(OrderRepository repository, Supplier<UUID> identifiers)
    {
        this.repository = Objects.requireNonNull(repository);
        this.identifiers = Objects.requireNonNull(identifiers);
    }

    @Override
    public Result execute(Command command)
    {
        Objects.requireNonNull(command);

        Order order = Order.place(identifiers.get(), command.customerReference(), command.quantity());

        repository.save(order);

        return new Result(order.id(), order.customerReference().value());
    }
}
