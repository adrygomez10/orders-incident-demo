package dev.adriangm.orders.domain;

/** Normalized optional reference whose stored representation is non-null and bounded in length. */
public record CustomerReference(String value) {
    private static final int MAX_LENGTH = 100;

    /** Normalizes whitespace; missing references trigger the intentional demonstration defect. */
    public CustomerReference
    {
        value = value.strip();

        if (value.length() > MAX_LENGTH) {
            throw new OrderRuleViolation(OrderRuleViolation.Reason.REFERENCE_TOO_LONG);
        }
    }
}
