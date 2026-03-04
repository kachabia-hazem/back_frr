package com.hazem.worklink.repositories;

import com.hazem.worklink.models.Contract;
import com.hazem.worklink.models.enums.ContractStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ContractRepository extends MongoRepository<Contract, String> {
    List<Contract> findByFreelancerIdOrderByCreatedAtDesc(String freelancerId);
    List<Contract> findByCompanyIdOrderByCreatedAtDesc(String companyId);
    boolean existsByJobIdAndFreelancerId(String jobId, String freelancerId);
    List<Contract> findByStatusAndCreatedAtBefore(ContractStatus status, LocalDateTime threshold);
}
