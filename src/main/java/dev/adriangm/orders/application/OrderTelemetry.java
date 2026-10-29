package dev.adriangm.orders.application;

/** Observation boundary separating request outcomes from the concrete monitoring provider. */
public interface OrderTelemetry {
    /**
     * Application outcome used to distinguish successful requests, rejected input and internal
     * failure.
     */
    enum Outcome {
        SUCCEEDED,
        REJECTED,
        FAILED
    }

    /** Synthetic caller and client-version labels attached to an order observation. */
    record Context(String userId, String clientVersion) {
    }

    /**
     * One request observation that can record a failure and must be finished exactly once by its
     * owner.
     */
    interface Observation {
        void recordFailure(RuntimeException failure);

        void finish(Outcome outcome);
    }

    /** Opens one observation for the supplied synthetic caller context. */
    Observation start(Context context);
}
