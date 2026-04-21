package com.griddynamics.reactive_paradigm_capstone.config;

import io.micrometer.context.ContextRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

@Configuration
public class PropagationConfig {

    public static final String REQUEST_ID = "requestId";

    @PostConstruct
    public void registerMdcAccessors() {
        Hooks.enableAutomaticContextPropagation();
        ContextRegistry.getInstance().registerThreadLocalAccessor(
            REQUEST_ID,
            () -> MDC.get(REQUEST_ID),
            requestId -> MDC.put(REQUEST_ID, requestId),
            () -> MDC.remove(REQUEST_ID)
        );
    }
}
