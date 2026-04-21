package com.griddynamics.reactive_paradigm_capstone.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean("orderSearchWebClient")
    public WebClient orderSearchWebClient(@Value("${services.order-search.base-url}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }

    @Bean("productInfoWebClient")
    public WebClient productInfoWebClient(@Value("${services.product-info.base-url}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}
