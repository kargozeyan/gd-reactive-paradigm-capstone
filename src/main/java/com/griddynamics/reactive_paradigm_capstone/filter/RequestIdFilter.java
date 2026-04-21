package com.griddynamics.reactive_paradigm_capstone.filter;

import static com.griddynamics.reactive_paradigm_capstone.config.PropagationConfig.REQUEST_ID;

import com.griddynamics.reactive_paradigm_capstone.util.RequestIdGenerator;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class RequestIdFilter implements WebFilter {

    private static final String REQUEST_ID_HEADER = "requestId";

    private final RequestIdGenerator idGenerator;

    public RequestIdFilter(RequestIdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String requestId = Optional.ofNullable(
            exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER)
        ).orElseGet(idGenerator::generate);

        return chain.filter(exchange).contextWrite(context -> context.put(REQUEST_ID, requestId));
    }
}
