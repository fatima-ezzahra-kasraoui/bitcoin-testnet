package com.bitcoin.bitcoin_testnet.repository;

import com.bitcoin.bitcoin_testnet.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends MongoRepository<Transaction, String> {
    List<Transaction> findByFromAddress(String fromAddress);
    List<Transaction> findByFromAddressOrToAddress(String fromAddress, String toAddress);
    Optional<Transaction> findByTxId(String txId);
    List<Transaction> findByStatus(String status);

    // AnomalyDetectionService — fromAddress-based for backward compat with existing data
    List<Transaction> findTop10ByFromAddressAndStatusOrderByCreatedAtDesc(String fromAddress, String status);
    List<Transaction> findByFromAddressAndToAddress(String fromAddress, String toAddress);
    long countByFromAddressAndCreatedAtAfter(String fromAddress, Date after);
    List<Transaction> findTop20ByFromAddressOrderByCreatedAtDesc(String fromAddress);

    // SecurityInsightsService
    List<Transaction> findByUserId(String userId);

    // Confirmation system
    List<Transaction> findByUserIdAndStatus(String userId, String status);
    List<Transaction> findByUserIdAndStatusAndConfirmationExpiresAtAfter(String userId, String status, LocalDateTime now);
    List<Transaction> findByStatusAndConfirmationExpiresAtBefore(String status, LocalDateTime now);
}
