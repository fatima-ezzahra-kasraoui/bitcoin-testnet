package com.bitcoin.bitcoin_testnet.repository;

import com.bitcoin.bitcoin_testnet.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Date;
import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByUserIdAndReadFalse(String userId);
    List<Notification> findByUserIdAndTypeAndCreatedAtAfter(String userId, String type, Date after);
    List<Notification> findTop20ByUserIdAndTypeOrderByCreatedAtDesc(String userId, String type);
}
