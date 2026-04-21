package com.griddynamics.reactive_paradigm_capstone.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "users")
public class UserInfo {

    @Id
    private String id;

    private String name;
    private String phone;
}
