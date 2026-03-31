package com.hazem.worklink.repositories;

import com.hazem.worklink.models.Deliverable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DeliverableRepository extends MongoRepository<Deliverable, String> {

    List<Deliverable> findByMissionIdOrderByUploadedAtDesc(String missionId);

    void deleteByMissionId(String missionId);
}
