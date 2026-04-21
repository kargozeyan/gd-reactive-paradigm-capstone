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

class ProductInfoClientTest {

    private WireMockServer server;
    private ProductInfoClient client;

    @BeforeEach
    void setUp() {
        server = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        server.start();

        WebClient webClient = WebClient.builder().baseUrl("http://localhost:" + server.port()).build();
        client = new ProductInfoClient(webClient);
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void itReturnsProductsForCode() {
        server.stubFor(
            get(urlPathEqualTo("/productInfoService/product/names"))
                .withQueryParam("productCode", equalTo("3852"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            [
                                {"productId":"1","productCode":"3852","productName":"IceCream","score":9000},
                                {"productId":"2","productCode":"3852","productName":"Milk","score":5000}
                            ]"""
                        )
                )
        );

        StepVerifier.create(client.getProductsByCode("3852"))
            .assertNext(p -> {
                assertThat(p.getProductId()).isEqualTo("1");
                assertThat(p.getProductName()).isEqualTo("IceCream");
                assertThat(p.getScore()).isEqualTo(9000);
            })
            .assertNext(p -> assertThat(p.getProductName()).isEqualTo("Milk"))
            .verifyComplete();
    }

    @Test
    void itReturnsEmptyFluxWhenServiceReturnsEmptyList() {
        server.stubFor(
            get(urlPathEqualTo("/productInfoService/product/names"))
                .withQueryParam("productCode", equalTo("0000"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("[]"))
        );

        StepVerifier.create(client.getProductsByCode("0000")).verifyComplete();
    }

    @Test
    void itReturnsEmptyFluxOnServerError() {
        server.stubFor(
            get(urlPathEqualTo("/productInfoService/product/names"))
                .withQueryParam("productCode", equalTo("3852"))
                .willReturn(aResponse().withStatus(503))
        );

        StepVerifier.create(client.getProductsByCode("3852")).verifyComplete();
    }

    @Test
    void itReturnsEmptyFluxOnTimeout() {
        server.stubFor(
            get(urlPathEqualTo("/productInfoService/product/names"))
                .withQueryParam("productCode", equalTo("3852"))
                .willReturn(
                    aResponse().withHeader("Content-Type", "application/json").withBody("[]").withFixedDelay(6000)
                )
        );

        StepVerifier.create(client.getProductsByCode("3852")).verifyComplete();
    }
}
