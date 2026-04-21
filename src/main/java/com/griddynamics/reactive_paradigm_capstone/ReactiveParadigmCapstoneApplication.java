package com.griddynamics.reactive_paradigm_capstone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Hooks;

@SpringBootApplication
public class ReactiveParadigmCapstoneApplication {

    public static void main(String[] args) {
        Hooks.enableAutomaticContextPropagation();
        SpringApplication.run(ReactiveParadigmCapstoneApplication.class, args);
    }
}
