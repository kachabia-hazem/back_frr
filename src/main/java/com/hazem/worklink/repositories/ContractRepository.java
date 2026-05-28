package com.hazem.worklink.repositories;

import com.hazem.worklink.models.Contract;
import com.hazem.worklink.models.enums.ContractStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ContractRepository extends MongoRepository<Contract, String> {
    List<Contract> findByFreelancerIdOrderByCreatedAtDesc(String freelancerId);
    List<Contract> findByCompanyIdOrderByCreatedAtDesc(String companyId);
    boolean existsByJobIdAndFreelancerId(String jobId, String freelancerId);
    List<Contract> findByStatusAndCreatedAtBefore(ContractStatus status, LocalDateTime threshold);
    List<Contract> findByStatusAndCreatedAtBetween(ContractStatus status, LocalDateTime from, LocalDateTime to);
    List<Contract> findByStatusAndStartDateLessThanEqual(ContractStatus status, LocalDate date);
    List<Contract> findByStatusAndStartDateBetween(ContractStatus status, LocalDate from, LocalDate to);
    boolean existsByCompanyIdAndFreelancerIdAndStatus(String companyId, String freelancerId, ContractStatus status);
    Optional<Contract> findByPaymentIntentId(String paymentIntentId);
}
