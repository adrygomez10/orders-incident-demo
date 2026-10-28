package dev.adriangm.orders.adapter.in.web;

import dev.adriangm.orders.application.OrderTelemetry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Bounds the observation lifecycle to one order HTTP request and records its final outcome. */
@Component
public final class OrderTelemetryFilter extends OncePerRequestFilter {
    static final String OBSERVATION = OrderTelemetryFilter.class.getName() + ".observation";
    private final OrderTelemetry telemetry;

    /** Receives the observation boundary used to instrument order requests. */
    public OrderTelemetryFilter(OrderTelemetry telemetry)
    {
        this.telemetry = telemetry;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request)
    {
        return !request.getRequestURI().equals("/orders") || !request.getMethod().equals("POST");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException
    {
        var observation = telemetry
            .start(
                DemoHeaders.safeObservation(request.getHeader("X-Demo-User"), request.getHeader("X-Client-Version"))
            );
        request.setAttribute(OBSERVATION, observation);
        boolean completed = false;
        try {
            chain.doFilter(request, response);
            completed = true;
        } finally {
            observation
                .finish(
                    !completed || response.getStatus() >= 500
                        ? OrderTelemetry.Outcome.FAILED
                        : response.getStatus() >= 400
                            ? OrderTelemetry.Outcome.REJECTED
                            : OrderTelemetry.Outcome.SUCCEEDED
                );
        }
    }
}
