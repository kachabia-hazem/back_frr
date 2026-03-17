package com.hazem.worklink.repositories;

import com.hazem.worklink.models.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    List<Message> findByConversationIdOrderByTimestampAsc(String conversationId);
}
