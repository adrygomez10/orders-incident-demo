package dev.adriangm.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.adriangm.orders.adapter.in.web.OrderController;
import dev.adriangm.orders.adapter.in.web.OrderExceptionHandler;
import dev.adriangm.orders.adapter.in.web.OrderTelemetryFilter;
import dev.adriangm.orders.application.OrderRepository;
import dev.adriangm.orders.application.OrderTelemetry;
import dev.adriangm.orders.application.PlaceOrder;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OrderHttpTest {
    private static String payload(int quantity, String reference)
    {
        var values = new java.util.LinkedHashMap<String, Object>();
        values.put("quantity", quantity);
        if (reference != null) {
            values.put("customerReference", reference);
        }
        return tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(values);
    }

    static final class RecordingTelemetry implements OrderTelemetry {
        Context context;
        Outcome outcome;
        RuntimeException error;
        int finishes;

        @Override
        public Observation start(Context value)
        {
            context = value;
            return new Observation() {
                @Override
                public void recordFailure(RuntimeException failure)
                {
                    error = failure;
                }

                @Override
                public void finish(Outcome value)
                {
                    outcome = value;
                    finishes++;
                }
            };
        }
    }

    MockMvc client(OrderRepository repository, RecordingTelemetry telemetry)
    {
        return MockMvcBuilders
            .standaloneSetup(new OrderController(new PlaceOrder(repository, UUID::randomUUID)))
            .setControllerAdvice(new OrderExceptionHandler())
            .addFilters(new OrderTelemetryFilter(telemetry))
            .build();
    }

    @Test
    void omittedHeadersUseValidDefaultsAndReturnTypedResponse() throws Exception
    {
        var telemetry = new RecordingTelemetry();
        client(order -> {
        }, telemetry)
            .perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(payload(1, "ORDER-1")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.customerReference").value("ORDER-1"));
        assertEquals("demo-user-0", telemetry.context.userId());
        assertEquals(OrderTelemetry.Outcome.SUCCEEDED, telemetry.outcome);
        assertEquals(1, telemetry.finishes);
    }

    @Test
    void invalidQuantityAndHeadersNeverReachPersistence() throws Exception
    {
        var telemetry = new RecordingTelemetry();
        var mvc = client(order -> fail("Invalid input persisted"), telemetry);
        mvc
            .perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(payload(0, "ORDER-1")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
        mvc
            .perform(
                post("/orders")
                    .header("X-Demo-User", "not-a-demo-user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload(1, "ORDER-1"))
            )
            .andExpect(status().isBadRequest());
        assertEquals(OrderTelemetry.Outcome.REJECTED, telemetry.outcome);
        assertNull(telemetry.error);
    }

    @Test
    void rejectsLongReferenceWithoutReportingServerFailure() throws Exception
    {
        var telemetry = new RecordingTelemetry();
        client(order -> fail("Invalid reference persisted"), telemetry)
            .perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(payload(1, "a".repeat(101))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("REFERENCE_TOO_LONG"));
        assertEquals(OrderTelemetry.Outcome.REJECTED, telemetry.outcome);
        assertEquals(1, telemetry.finishes);
    }

    @Test
    void storageFailuresAreReportedWithoutLeakingDetailsAndObservationAlwaysCloses() throws Exception
    {
        var telemetry = new RecordingTelemetry();
        var failure = new IllegalStateException("private database detail");
        client(order -> {
            throw failure;
        }, telemetry)
            .perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(payload(1, "ORDER-1")))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("ORDER_SAVE_FAILED"));
        assertSame(failure, telemetry.error);
        assertEquals(OrderTelemetry.Outcome.FAILED, telemetry.outcome);
        assertEquals(1, telemetry.finishes);
    }

    @Test
    void missingReferenceFailureRemainsReproducible() throws Exception
    {
        var telemetry = new RecordingTelemetry();
        client(order -> {
        }, telemetry)
            .perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(payload(1, null)))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("ORDER_SAVE_FAILED"));
        assertInstanceOf(NullPointerException.class, telemetry.error);
        assertEquals(OrderTelemetry.Outcome.FAILED, telemetry.outcome);
    }
}
