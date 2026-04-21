package com.griddynamics.reactive_paradigm_capstone.client;

import com.griddynamics.reactive_paradigm_capstone.domain.Product;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class ProductInfoClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public ProductInfoClient(@Qualifier("productInfoWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Flux<Product> getProductsByCode(String productCode) {
        return webClient
            .get()
            .uri(uriBuilder ->
                uriBuilder.path("/productInfoService/product/names").queryParam("productCode", productCode).build()
            )
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<Product>>() {})
            .timeout(TIMEOUT)
            .doOnNext(products ->
                log.info("Received {} products from Product Info service for code {}", products.size(), productCode)
            )
            .doOnError(e -> log.error("Error from Product Info service for code {}: {}", productCode, e.getMessage()))
            .flatMapIterable(products -> products)
            .onErrorResume(e -> Mono.empty());
    }
}
