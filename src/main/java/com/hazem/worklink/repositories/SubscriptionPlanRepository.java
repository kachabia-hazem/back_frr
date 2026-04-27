package com.hazem.worklink.repositories;

import com.hazem.worklink.models.SubscriptionPlan;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SubscriptionPlanRepository extends MongoRepository<SubscriptionPlan, String> {
    List<SubscriptionPlan> findAllByOrderByDisplayOrderAsc();
}
