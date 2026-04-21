package com.griddynamics.reactive_paradigm_capstone.controller;

import com.griddynamics.reactive_paradigm_capstone.domain.AggregatedOrder;
import com.griddynamics.reactive_paradigm_capstone.service.OrderAggregatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderAggregatorController {

    private final OrderAggregatorService orderAggregatorService;

    @GetMapping(value = "/{userId}", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<AggregatedOrder> getOrders(@PathVariable String userId) {
        return orderAggregatorService.getOrdersByUserId(userId);
    }
}
