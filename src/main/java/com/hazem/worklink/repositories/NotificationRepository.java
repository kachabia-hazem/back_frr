package com.hazem.worklink.repositories;

import com.hazem.worklink.models.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId);

    long countByRecipientIdAndIsReadFalse(String recipientId);

    List<Notification> findByRecipientIdAndIsReadFalse(String recipientId);
}
