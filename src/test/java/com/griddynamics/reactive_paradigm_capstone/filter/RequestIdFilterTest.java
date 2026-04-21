package com.griddynamics.reactive_paradigm_capstone.filter;

import static com.griddynamics.reactive_paradigm_capstone.config.PropagationConfig.REQUEST_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.griddynamics.reactive_paradigm_capstone.util.RequestIdGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class RequestIdFilterTest {

    @Mock
    private RequestIdGenerator idGenerator;

    @InjectMocks
    private RequestIdFilter filter;

    @Test
    void itPutsRequestIdHeaderValueIntoContext() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/").header("requestId", "test-request-id").build()
        );

        filter
            .filter(exchange, ex ->
                Mono.deferContextual(ctx -> {
                    String requestId = ctx.get(REQUEST_ID);
                    assertThat(requestId).isEqualTo("test-request-id");
                    return Mono.empty();
                })
            )
            .block();
    }

    @Test
    void itUsesGeneratedIdWhenRequestIdHeaderIsMissing() {
        when(idGenerator.generate()).thenReturn("generated-id");
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build());

        filter
            .filter(exchange, ex ->
                Mono.deferContextual(ctx -> {
                    String requestId = ctx.get(REQUEST_ID);
                    assertThat(requestId).isEqualTo("generated-id");
                    return Mono.empty();
                })
            )
            .block();
    }

    @Test
    void itGeneratesDifferentIdForEachRequestWithoutHeader() {
        when(idGenerator.generate()).thenReturn("id-1", "id-2");

        filter
            .filter(MockServerWebExchange.from(MockServerHttpRequest.get("/").build()), ex ->
                Mono.deferContextual(ctx -> {
                    String requestId = ctx.get(REQUEST_ID);
                    assertThat(requestId).isEqualTo("id-1");
                    return Mono.empty();
                })
            )
            .block();

        filter
            .filter(MockServerWebExchange.from(MockServerHttpRequest.get("/").build()), ex ->
                Mono.deferContextual(ctx -> {
                    String requestId = ctx.get(REQUEST_ID);
                    assertThat(requestId).isEqualTo("id-2");
                    return Mono.empty();
                })
            )
            .block();
    }
}
