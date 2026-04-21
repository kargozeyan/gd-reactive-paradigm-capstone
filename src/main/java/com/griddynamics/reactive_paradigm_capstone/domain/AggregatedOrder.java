package com.griddynamics.reactive_paradigm_capstone.domain;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregatedOrder {

    private String orderNumber;
    private String userName;
    private String phoneNumber;
    private String productCode;
    private Optional<String> productName;
    private Optional<String> productId;
}
