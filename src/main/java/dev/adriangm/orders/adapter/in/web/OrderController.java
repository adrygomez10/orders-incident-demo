package dev.adriangm.orders.adapter.in.web;

import dev.adriangm.orders.application.PlaceOrderUseCase;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Maps order requests to the placement use case and presents a typed HTTP response. */
@RestController
public final class OrderController {
  private final PlaceOrderUseCase placeOrder;

  /**
   * Receives the order-placement use case without constructing persistence or telemetry adapters.
   */
  public OrderController(PlaceOrderUseCase placeOrder) {
    this.placeOrder = placeOrder;
  }

  /** HTTP order input retaining the optional customer reference and requested quantity. */
  public record OrderRequest(String customerReference, int quantity) {}

  /** Public representation of a newly persisted order, without persistence or telemetry details. */
  public record OrderResponse(UUID id, String customerReference) {}

  /**
   * Validates demonstration headers and delegates placement without using SQL or a monitoring SDK.
   */
  @PostMapping("/orders")
  public ResponseEntity<OrderResponse> save(
      @RequestBody OrderRequest request,
      @RequestHeader(value = "X-Demo-User", defaultValue = DemoHeaders.DEFAULT_USER) String user,
      @RequestHeader(value = "X-Client-Version", defaultValue = DemoHeaders.DEFAULT_CLIENT)
          String client) {
    DemoHeaders.requireValid(user, client);

    PlaceOrderUseCase.Result placed =
        placeOrder.execute(
            new PlaceOrderUseCase.Command(request.customerReference(), request.quantity()));

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new OrderResponse(placed.id(), placed.customerReference()));
  }
}
