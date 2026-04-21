package com.griddynamics.reactive_paradigm_capstone.util;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RequestIdGenerator {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
