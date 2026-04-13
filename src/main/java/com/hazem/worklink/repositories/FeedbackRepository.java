package com.hazem.worklink.repositories;

import com.hazem.worklink.models.Feedback;
import com.hazem.worklink.models.enums.FeedbackStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FeedbackRepository extends MongoRepository<Feedback, String> {
    List<Feedback> findAllByOrderByCreatedAtDesc();
    List<Feedback> findByStatusOrderByCreatedAtDesc(FeedbackStatus status);
    boolean existsByMissionIdAndUserId(String missionId, String userId);
}
