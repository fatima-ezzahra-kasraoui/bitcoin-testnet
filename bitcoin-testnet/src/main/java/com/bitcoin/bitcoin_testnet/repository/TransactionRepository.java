package com.bitcoin.bitcoin_testnet.repository;

import com.bitcoin.bitcoin_testnet.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends MongoRepository<Transaction, String> {
    List<Transaction> findByFromAddress(String fromAddress);
    List<Transaction> findByFromAddressOrToAddress(String fromAddress, String toAddress);
    Optional<Transaction> findByTxId(String txId);
}