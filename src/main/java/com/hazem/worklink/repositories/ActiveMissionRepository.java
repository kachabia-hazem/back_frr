package com.hazem.worklink.repositories;

import com.hazem.worklink.models.ActiveMission;
import com.hazem.worklink.models.enums.ActiveMissionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ActiveMissionRepository extends MongoRepository<ActiveMission, String> {

    Optional<ActiveMission> findByContractId(String contractId);

    List<ActiveMission> findByFreelancerIdOrderByCreatedAtDesc(String freelancerId);

    List<ActiveMission> findByCompanyIdOrderByCreatedAtDesc(String companyId);

    List<ActiveMission> findByCompanyIdAndStatusOrderBySubmittedAtDesc(String companyId, ActiveMissionStatus status);

    /** Returns PENDING missions whose startDate has arrived (startDate <= today). */
    List<ActiveMission> findByStatusAndStartDateLessThanEqual(ActiveMissionStatus status, LocalDate date);
}
