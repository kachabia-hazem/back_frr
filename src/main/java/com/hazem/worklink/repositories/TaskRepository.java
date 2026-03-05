package com.hazem.worklink.repositories;

import com.hazem.worklink.models.Task;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TaskRepository extends MongoRepository<Task, String> {

    List<Task> findByMissionIdOrderByOrderIndexAsc(String missionId);

    void deleteByMissionId(String missionId);
}
