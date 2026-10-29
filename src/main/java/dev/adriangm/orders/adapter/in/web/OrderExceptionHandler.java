package dev.adriangm.orders.adapter.in.web;

import dev.adriangm.orders.application.OrderTelemetry;
import dev.adriangm.orders.domain.OrderRuleViolation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps order-rule rejections and unexpected failures to typed HTTP error responses. */
@RestControllerAdvice(assignableTypes = OrderController.class)
public final class OrderExceptionHandler {
    /** HTTP error code without internal exception details or credentials. */
    public record ErrorResponse(String error) {
    }

    /** Translates a rejected order invariant to its stable HTTP input error. */
    @ExceptionHandler(OrderRuleViolation.class)
    public ResponseEntity<ErrorResponse> invalidOrder(OrderRuleViolation failure)
    {
        String code = switch (failure.reason()) {
            case INVALID_QUANTITY -> "INVALID_REQUEST";
            case REFERENCE_TOO_LONG -> "REFERENCE_TOO_LONG";
        };
        return ResponseEntity.badRequest().body(new ErrorResponse(code));
    }

    /** Reports malformed input without treating it as an internal application failure. */
    @ExceptionHandler({DemoHeaders.InvalidDemoHeaders.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> invalidInput()
    {
        return ResponseEntity.badRequest().body(new ErrorResponse("INVALID_REQUEST"));
    }

    /** Records an unexpected failure and returns a public error without leaking its details. */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> unexpectedFailure(RuntimeException failure, HttpServletRequest request)
    {
        Object observation = request.getAttribute(OrderTelemetryFilter.OBSERVATION);
        if (observation instanceof OrderTelemetry.Observation operation) {
            operation.recordFailure(failure);
        }
        return ResponseEntity.internalServerError().body(new ErrorResponse("ORDER_SAVE_FAILED"));
    }
}
