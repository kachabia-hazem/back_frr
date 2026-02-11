package com.hazem.worklink.repositories;

import com.hazem.worklink.models.Application;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends MongoRepository<Application, String> {
    List<Application> findByFreelancerId(String freelancerId);
    List<Application> findByMissionId(String missionId);
    Optional<Application> findByFreelancerIdAndMissionId(String freelancerId, String missionId);
}
