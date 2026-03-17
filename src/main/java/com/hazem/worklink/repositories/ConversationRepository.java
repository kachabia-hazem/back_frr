package com.hazem.worklink.repositories;

import com.hazem.worklink.models.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {
    Optional<Conversation> findByCompanyIdAndFreelancerId(String companyId, String freelancerId);
    List<Conversation> findByCompanyId(String companyId);
    List<Conversation> findByFreelancerId(String freelancerId);
}
