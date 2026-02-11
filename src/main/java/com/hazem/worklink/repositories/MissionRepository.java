package com.hazem.worklink.repositories;

import com.hazem.worklink.models.Mission;
import com.hazem.worklink.models.enums.MissionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MissionRepository extends MongoRepository<Mission, String> {
    List<Mission> findByCompanyId(String companyId);
    List<Mission> findByStatus(MissionStatus status);
    List<Mission> findByCompanyIdAndStatus(String companyId, MissionStatus status);
    List<Mission> findByStatusAndApplicationDeadlineBefore(MissionStatus status, LocalDate date);
}
