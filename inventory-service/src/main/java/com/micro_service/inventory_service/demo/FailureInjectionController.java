package com.micro_service.inventory_service.demo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo/failures")
@Profile("demo")
@Validated
public class FailureInjectionController {

    private final FailureMode failureMode;

    public FailureInjectionController(FailureMode failureMode) {
        this.failureMode = failureMode;
    }

    @PostMapping
    public FailureMode.State configure(
            @RequestParam(defaultValue = "0") @Min(0) @Max(10_000) long latencyMillis,
            @RequestParam(defaultValue = "false") boolean failRequests) {
        failureMode.configure(latencyMillis, failRequests);
        return failureMode.state();
    }

    @DeleteMapping
    public FailureMode.State reset() {
        failureMode.configure(0, false);
        return failureMode.state();
    }
}
