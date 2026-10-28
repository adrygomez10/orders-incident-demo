package dev.adriangm.orders.domain;

/** Order quantity restricted to the accepted inclusive business interval. */
public record OrderQuantity(int value) {
    private static final int MINIMUM = 1;
    private static final int MAXIMUM = 100;

    /** Rejects quantities outside the permitted business interval. */
    public OrderQuantity
    {
        if (value < MINIMUM || value > MAXIMUM) {
            throw new OrderRuleViolation(OrderRuleViolation.Reason.INVALID_QUANTITY);
        }
    }
}
