package com.micro_service.inventory_service.demo;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class FailureMode {

    private final AtomicLong latencyMillis = new AtomicLong();
    private final AtomicBoolean errorsEnabled = new AtomicBoolean();

    public void apply() {
        if (errorsEnabled.get()) {
            throw new IllegalStateException("Demo incident: inventory dependency is unavailable");
        }

        long delay = latencyMillis.get();
        if (delay > 0) {
            try {
                Thread.sleep(Duration.ofMillis(delay));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Demo latency was interrupted", interrupted);
            }
        }
    }

    public void configure(long delayMillis, boolean failRequests) {
        latencyMillis.set(Math.max(0, Math.min(delayMillis, 10_000)));
        errorsEnabled.set(failRequests);
    }

    public State state() {
        return new State(latencyMillis.get(), errorsEnabled.get());
    }

    public record State(long latencyMillis, boolean failRequests) {
    }
}
