package com.griddynamics.reactive_paradigm_capstone.service;

import com.griddynamics.reactive_paradigm_capstone.client.OrderSearchClient;
import com.griddynamics.reactive_paradigm_capstone.client.ProductInfoClient;
import com.griddynamics.reactive_paradigm_capstone.domain.AggregatedOrder;
import com.griddynamics.reactive_paradigm_capstone.domain.Order;
import com.griddynamics.reactive_paradigm_capstone.domain.Product;
import com.griddynamics.reactive_paradigm_capstone.repository.UserInfoRepository;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAggregatorService {

    private final UserInfoRepository userInfoRepository;
    private final OrderSearchClient orderSearchClient;
    private final ProductInfoClient productInfoClient;

    public Flux<AggregatedOrder> getOrdersByUserId(String userId) {
        return userInfoRepository
            .findById(userId)
            .doOnNext(user -> log.info("Found user {} with phone {}", user.getId(), user.getPhone()))
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId)))
            .flatMapMany(user ->
                orderSearchClient
                    .getOrdersByPhone(user.getPhone())
                    .flatMap(order -> enrichWithProduct(order, user.getName()))
            );
    }

    private Mono<AggregatedOrder> enrichWithProduct(Order order, String userName) {
        return productInfoClient
            .getProductsByCode(order.getProductCode())
            .collectList()
            .map(products -> products.stream().max(Comparator.comparingDouble(Product::getScore)))
            .map(best ->
                AggregatedOrder.builder()
                    .orderNumber(order.getOrderNumber())
                    .userName(userName)
                    .phoneNumber(order.getPhoneNumber())
                    .productCode(order.getProductCode())
                    .productName(best.map(Product::getProductName))
                    .productId(best.map(Product::getProductId))
                    .build()
            )
            .doOnNext(aggregated -> log.info("Aggregated order: {}", aggregated));
    }
}
