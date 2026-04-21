package com.griddynamics.reactive_paradigm_capstone.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

class OrderSearchClientTest {

    private WireMockServer server;
    private OrderSearchClient client;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        server.start();

        WebClient webClient = WebClient.builder().baseUrl("http://localhost:" + server.port()).build();
        client = new OrderSearchClient(webClient);
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void itReturnsStreamOfOrders() {
        server.stubFor(
            get(urlPathEqualTo("/orderSearchService/order/phone"))
                .withQueryParam("phoneNumber", equalTo("123456789"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/x-ndjson")
                        .withBody(
                            """
                            {"phoneNumber":"123456789","orderNumber":"ORD-1","productCode":"3852"}
                            {"phoneNumber":"123456789","orderNumber":"ORD-2","productCode":"5256"}
                            """
                        )
                )
        );

        StepVerifier.create(client.getOrdersByPhone("123456789"))
            .assertNext(order -> {
                assertThat(order.getOrderNumber()).isEqualTo("ORD-1");
                assertThat(order.getProductCode()).isEqualTo("3852");
                assertThat(order.getPhoneNumber()).isEqualTo("123456789");
            })
            .assertNext(order -> {
                assertThat(order.getOrderNumber()).isEqualTo("ORD-2");
                assertThat(order.getProductCode()).isEqualTo("5256");
            })
            .verifyComplete();
    }

    @Test
    void itReturnsEmptyStreamWhenNoOrders() {
        server.stubFor(
            get(urlPathEqualTo("/orderSearchService/order/phone"))
                .withQueryParam("phoneNumber", equalTo("000000000"))
                .willReturn(aResponse().withHeader("Content-Type", "application/x-ndjson").withBody(""))
        );

        StepVerifier.create(client.getOrdersByPhone("000000000")).verifyComplete();
    }

    @Test
    void itPropagatesErrorOnServerFailure() {
        server.stubFor(
            get(urlPathEqualTo("/orderSearchService/order/phone"))
                .withQueryParam("phoneNumber", equalTo("123456789"))
                .willReturn(aResponse().withStatus(500))
        );

        StepVerifier.create(client.getOrdersByPhone("123456789")).expectError().verify();
    }
}
