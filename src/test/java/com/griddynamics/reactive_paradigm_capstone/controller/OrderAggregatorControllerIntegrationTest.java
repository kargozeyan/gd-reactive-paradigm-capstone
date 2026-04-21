package com.griddynamics.reactive_paradigm_capstone.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.griddynamics.reactive_paradigm_capstone.domain.AggregatedOrder;
import com.griddynamics.reactive_paradigm_capstone.domain.UserInfo;
import com.griddynamics.reactive_paradigm_capstone.repository.UserInfoRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderAggregatorControllerIntegrationTest {

    static WireMockServer orderSearchServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
    static WireMockServer productInfoServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

    // Mocking only DB layer
    @MockitoBean
    UserInfoRepository userInfoRepository;

    @LocalServerPort
    int port;

    WebTestClient webTestClient;

    @DynamicPropertySource
    static void overrideServiceUrls(DynamicPropertyRegistry registry) {
        orderSearchServer.start();
        productInfoServer.start();

        registry.add("services.order-search.base-url", () -> "http://localhost:" + orderSearchServer.port());
        registry.add("services.product-info.base-url", () -> "http://localhost:" + productInfoServer.port());
    }

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();

        orderSearchServer.resetAll();
        productInfoServer.resetAll();
    }

    @AfterAll
    static void tearDown() {
        orderSearchServer.stop();
        productInfoServer.stop();
    }

    @Test
    void itReturnsAggregatedOrders() {
        when(userInfoRepository.findById("user1")).thenReturn(Mono.just(user("user1", "John", "123456789")));

        orderSearchServer.stubFor(
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

        productInfoServer.stubFor(
            get(urlPathEqualTo("/productInfoService/product/names"))
                .withQueryParam("productCode", equalTo("3852"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            [{"productId":"1","productCode":"3852","productName":"IceCream","score":9000}]
                            """
                        )
                )
        );

        productInfoServer.stubFor(
            get(urlPathEqualTo("/productInfoService/product/names"))
                .withQueryParam("productCode", equalTo("5256"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            [{"productId":"3","productCode":"5256","productName":"Apple","score":7500}]
                            """
                        )
                )
        );

        webTestClient
            .get()
            .uri("/orders/user1")
            .header("requestId", "req-integration-1")
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
            .expectBodyList(AggregatedOrder.class)
            .hasSize(2)
            .value(orders -> {
                assertThat(orders).anySatisfy(o -> {
                    assertThat(o.getOrderNumber()).isEqualTo("ORD-1");
                    assertThat(o.getUserName()).isEqualTo("John");
                    assertThat(o.getProductName()).contains("IceCream");
                });
                assertThat(orders).anySatisfy(o -> {
                    assertThat(o.getOrderNumber()).isEqualTo("ORD-2");
                    assertThat(o.getUserName()).isEqualTo("John");
                    assertThat(o.getProductName()).contains("Apple");
                });
            });
    }

    @Test
    void itReturnsOrdersWithEmptyProductFieldsWhenProductInfoIsDown() {
        when(userInfoRepository.findById("user1")).thenReturn(Mono.just(user("user1", "John", "123456789")));

        orderSearchServer.stubFor(
            get(urlPathEqualTo("/orderSearchService/order/phone"))
                .withQueryParam("phoneNumber", equalTo("123456789"))
                .willReturn(
                    aResponse()
                        .withHeader("Content-Type", "application/x-ndjson")
                        .withBody(
                            """
                            {"phoneNumber":"123456789","orderNumber":"ORD-1","productCode":"3852"}
                            """
                        )
                )
        );

        productInfoServer.stubFor(
            get(urlPathEqualTo("/productInfoService/product/names"))
                .withQueryParam("productCode", equalTo("3852"))
                .willReturn(aResponse().withStatus(503))
        );

        webTestClient
            .get()
            .uri("/orders/user1")
            .header("requestId", "req-integration-2")
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBodyList(AggregatedOrder.class)
            .hasSize(1)
            .value(orders -> {
                AggregatedOrder order = orders.getFirst();
                assertThat(order.getOrderNumber()).isEqualTo("ORD-1");
                assertThat(order.getProductName()).isEmpty();
                assertThat(order.getProductId()).isEmpty();
            });
    }

    @Test
    void itReturns404WhenUserNotFound() {
        when(userInfoRepository.findById("unknown")).thenReturn(Mono.empty());

        webTestClient
            .get()
            .uri("/orders/unknown")
            .header("requestId", "req-integration-3")
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    private UserInfo user(String id, String name, String phone) {
        UserInfo u = new UserInfo();
        u.setId(id);
        u.setName(name);
        u.setPhone(phone);
        return u;
    }
}
