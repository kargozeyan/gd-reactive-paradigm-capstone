package com.griddynamics.reactive_paradigm_capstone.repository;

import com.griddynamics.reactive_paradigm_capstone.domain.UserInfo;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserInfoRepository extends ReactiveMongoRepository<UserInfo, String> {}
