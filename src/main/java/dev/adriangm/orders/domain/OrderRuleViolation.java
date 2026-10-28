package dev.adriangm.orders.domain;

/** Rejected order invariant exposed independently of its eventual HTTP representation. */
public final class OrderRuleViolation extends RuntimeException {
  /** Business reasons for rejecting an order before persistence. */
  public enum Reason {
    INVALID_QUANTITY,
    REFERENCE_TOO_LONG
  }

  private final Reason reason;

  /** Retains the violated invariant without assigning it an HTTP status. */
  public OrderRuleViolation(Reason reason) {
    super(reason.name());
    this.reason = reason;
  }

  /** Returns the rejected invariant independently of HTTP error mapping. */
  public Reason reason() {
    return reason;
  }
}
