package com.hazem.worklink.repositories;

import com.hazem.worklink.models.ActiveMission;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ActiveMissionRepository extends MongoRepository<ActiveMission, String> {

    Optional<ActiveMission> findByContractId(String contractId);

    List<ActiveMission> findByFreelancerIdOrderByCreatedAtDesc(String freelancerId);

    List<ActiveMission> findByCompanyIdOrderByCreatedAtDesc(String companyId);
}
