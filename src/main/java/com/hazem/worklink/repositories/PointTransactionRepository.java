package com.hazem.worklink.repositories;

import com.hazem.worklink.models.PointTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointTransactionRepository extends MongoRepository<PointTransaction, String> {
    List<PointTransaction> findByUserIdOrderByCreatedAtDesc(String userId);
    boolean existsByReferenceId(String referenceId);
}
