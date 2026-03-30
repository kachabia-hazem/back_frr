package com.hazem.worklink.repositories;

import com.hazem.worklink.models.Review;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReviewRepository extends MongoRepository<Review, String> {
    List<Review> findByFreelancerIdOrderByCreatedAtDesc(String freelancerId);
    boolean existsByMissionId(String missionId);
}
