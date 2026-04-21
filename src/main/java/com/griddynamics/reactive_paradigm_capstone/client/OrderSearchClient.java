package com.griddynamics.reactive_paradigm_capstone.client;

import com.griddynamics.reactive_paradigm_capstone.domain.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Slf4j
@Component
public class OrderSearchClient {

    private final WebClient webClient;

    public OrderSearchClient(@Qualifier("orderSearchWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Flux<Order> getOrdersByPhone(String phoneNumber) {
        return webClient
            .get()
            .uri(uriBuilder ->
                uriBuilder.path("/orderSearchService/order/phone").queryParam("phoneNumber", phoneNumber).build()
            )
            .accept(MediaType.APPLICATION_NDJSON)
            .retrieve()
            .bodyToFlux(Order.class)
            .doOnNext(order -> log.info("Received order from Order Search service: {}", order))
            .doOnError(e -> log.error("Error from Order Search service: {}", e.getMessage()));
    }
}
