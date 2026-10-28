package dev.adriangm.orders.adapter.in.web;

import dev.adriangm.orders.application.OrderTelemetry.Context;
import java.util.regex.Pattern;

final class DemoHeaders {
  static final String DEFAULT_USER = "demo-user-0";
  static final String DEFAULT_CLIENT = "unknown";
  private static final Pattern USER = Pattern.compile("demo-user-\\d+");
  private static final Pattern CLIENT = Pattern.compile("[A-Za-z0-9_.-]{1,30}");

  private DemoHeaders() {}

  static void requireValid(String user, String client) {
    if (!USER.matcher(user).matches() || !CLIENT.matcher(client).matches()) {
      throw new InvalidDemoHeaders();
    }
  }

  static Context safeObservation(String user, String client) {
    return new Context(
        user != null && USER.matcher(user).matches() ? user : DEFAULT_USER,
        client != null && CLIENT.matcher(client).matches() ? client : DEFAULT_CLIENT);
  }

  static final class InvalidDemoHeaders extends RuntimeException {}
}
