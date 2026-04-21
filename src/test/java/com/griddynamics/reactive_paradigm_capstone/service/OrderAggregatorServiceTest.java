package com.griddynamics.reactive_paradigm_capstone.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.griddynamics.reactive_paradigm_capstone.client.OrderSearchClient;
import com.griddynamics.reactive_paradigm_capstone.client.ProductInfoClient;
import com.griddynamics.reactive_paradigm_capstone.domain.Order;
import com.griddynamics.reactive_paradigm_capstone.domain.Product;
import com.griddynamics.reactive_paradigm_capstone.domain.UserInfo;
import com.griddynamics.reactive_paradigm_capstone.repository.UserInfoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class OrderAggregatorServiceTest {

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private OrderSearchClient orderSearchClient;

    @Mock
    private ProductInfoClient productInfoClient;

    @InjectMocks
    private OrderAggregatorService service;

    @Test
    void itAggregatesOrdersWithHighestScoringProduct() {
        when(userInfoRepository.findById("user1")).thenReturn(Mono.just(user("user1", "John", "123456789")));
        when(orderSearchClient.getOrdersByPhone("123456789")).thenReturn(
            Flux.just(order("ORD-1", "3852", "123456789"), order("ORD-2", "5256", "123456789"))
        );
        when(productInfoClient.getProductsByCode("3852")).thenReturn(
            Flux.just(product("1", "3852", "IceCream", 9000), product("2", "3852", "Milk", 5000))
        );
        when(productInfoClient.getProductsByCode("5256")).thenReturn(Flux.just(product("3", "5256", "Apple", 7500)));

        StepVerifier.create(service.getOrdersByUserId("user1").collectList())
            .assertNext(orders -> {
                assertThat(orders).hasSize(2);
                assertThat(orders).anySatisfy(o -> {
                    assertThat(o.getOrderNumber()).isEqualTo("ORD-1");
                    assertThat(o.getUserName()).isEqualTo("John");
                    assertThat(o.getPhoneNumber()).isEqualTo("123456789");
                    assertThat(o.getProductCode()).isEqualTo("3852");
                    assertThat(o.getProductName()).contains("IceCream");
                    assertThat(o.getProductId()).contains("1");
                });
                assertThat(orders).anySatisfy(o -> {
                    assertThat(o.getOrderNumber()).isEqualTo("ORD-2");
                    assertThat(o.getProductName()).contains("Apple");
                    assertThat(o.getProductId()).contains("3");
                });
            })
            .verifyComplete();
    }

    @Test
    void itReturnsOrderWithEmptyProductFieldsWhenProductInfoReturnsEmpty() {
        when(userInfoRepository.findById("user1")).thenReturn(Mono.just(user("user1", "John", "123456789")));
        when(orderSearchClient.getOrdersByPhone("123456789")).thenReturn(
            Flux.just(order("ORD-1", "3852", "123456789"))
        );
        when(productInfoClient.getProductsByCode("3852")).thenReturn(Flux.empty());

        StepVerifier.create(service.getOrdersByUserId("user1"))
            .assertNext(o -> {
                assertThat(o.getOrderNumber()).isEqualTo("ORD-1");
                assertThat(o.getProductName()).isEmpty();
                assertThat(o.getProductId()).isEmpty();
            })
            .verifyComplete();
    }

    @Test
    void itThrowsErrorWhenUserNotFound() {
        when(userInfoRepository.findById("unknown")).thenReturn(Mono.empty());

        StepVerifier.create(service.getOrdersByUserId("unknown"))
            .expectErrorMatches(
                e ->
                    e instanceof ResponseStatusException &&
                    ((ResponseStatusException) e).getStatusCode() == HttpStatus.NOT_FOUND
            )
            .verify();
    }

    @Test
    void itReturnsEmptyStreamWhenNoOrdersFound() {
        when(userInfoRepository.findById("user1")).thenReturn(Mono.just(user("user1", "John", "123456789")));
        when(orderSearchClient.getOrdersByPhone("123456789")).thenReturn(Flux.empty());

        StepVerifier.create(service.getOrdersByUserId("user1")).verifyComplete();
    }

    @Test
    void itPicksProductWithHighestScore() {
        when(userInfoRepository.findById("user1")).thenReturn(Mono.just(user("user1", "John", "123456789")));
        when(orderSearchClient.getOrdersByPhone("123456789")).thenReturn(
            Flux.just(order("ORD-1", "3852", "123456789"))
        );
        when(productInfoClient.getProductsByCode("3852")).thenReturn(
            Flux.just(
                product("1", "3852", "Milk", 5000),
                product("2", "3852", "IceCream", 9000),
                product("3", "3852", "Apple", 3000)
            )
        );

        StepVerifier.create(service.getOrdersByUserId("user1"))
            .assertNext(o -> {
                assertThat(o.getProductName()).contains("IceCream");
                assertThat(o.getProductId()).contains("2");
            })
            .verifyComplete();
    }

    private UserInfo user(String id, String name, String phone) {
        UserInfo u = new UserInfo();
        u.setId(id);
        u.setName(name);
        u.setPhone(phone);
        return u;
    }

    private Order order(String orderNumber, String productCode, String phoneNumber) {
        Order o = new Order();
        o.setOrderNumber(orderNumber);
        o.setProductCode(productCode);
        o.setPhoneNumber(phoneNumber);
        return o;
    }

    private Product product(String id, String code, String name, double score) {
        Product p = new Product();
        p.setProductId(id);
        p.setProductCode(code);
        p.setProductName(name);
        p.setScore(score);
        return p;
    }
}
