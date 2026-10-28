package dev.adriangm.orders.adapter.out.telemetry;

import dev.adriangm.orders.application.OrderTelemetry;
import io.sentry.ITransaction;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SpanStatus;
import io.sentry.protocol.User;

/** Captures synthetic order failures and traces without introducing Sentry types into the core. */
public final class SentryOrderTelemetry implements OrderTelemetry, AutoCloseable {
  private final String run;
  private final boolean enabled;

  /** Initializes optional Sentry capture with a release identity and synthetic run label. */
  public SentryOrderTelemetry(String dsn, String release, String run) {
    this.run = run;
    this.enabled = !dsn.isBlank();
    if (enabled) {
      Sentry.init(
          options -> {
            options.setDsn(dsn);
            options.setRelease(release);
            options.setEnvironment("workshop");
            options.setTracesSampleRate(1.0);
            options.setSendDefaultPii(false);
          });
    }
  }

  @Override
  public Observation start(Context context) {
    ITransaction transaction = Sentry.startTransaction("POST /orders", "http.server");
    return new Observation() {
      @Override
      public void recordFailure(RuntimeException failure) {
        Sentry.withScope(
            scope -> {
              User user = new User();
              user.setId(context.userId());
              scope.setUser(user);
              scope.setTag("component", "order-api");
              scope.setTag("client_version", context.clientVersion());
              scope.setTag("demo_run", run);
              scope.setTransaction("POST /orders");

              SentryEvent event = new SentryEvent(failure);
              event.getContexts().setTrace(transaction.getSpanContext());

              Sentry.captureEvent(event);
            });
      }

      @Override
      public void finish(Outcome outcome) {
        transaction.setStatus(
            switch (outcome) {
              case SUCCEEDED -> SpanStatus.OK;
              case REJECTED -> SpanStatus.INVALID_ARGUMENT;
              case FAILED -> SpanStatus.INTERNAL_ERROR;
            });
        transaction.finish();
      }
    };
  }

  @Override
  public void close() {
    if (enabled) {
      Sentry.close();
    }
  }
}
