package com.hazem.worklink.repositories;

import com.hazem.worklink.models.Legit;
import com.hazem.worklink.models.enums.LegitStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface LegitRepository extends MongoRepository<Legit, String> {
    List<Legit> findAllByOrderByCreatedAtDesc();
    List<Legit> findByStatusOrderByCreatedAtDesc(LegitStatus status);
    long countByStatus(LegitStatus status);
}
