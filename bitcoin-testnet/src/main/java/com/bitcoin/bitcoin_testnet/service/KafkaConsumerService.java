package com.bitcoin.bitcoin_testnet.service;

import com.bitcoin.bitcoin_testnet.model.Transaction;
import com.bitcoin.bitcoin_testnet.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final TransactionRepository transactionRepository;

    @KafkaListener(topics = "blockchain-transactions", groupId = "bitcoin-group")
    public void listen(String txId) {
        System.out.println("Event reçu de Kafka : " + txId);

        Optional<Transaction> txOpt = transactionRepository.findByTxId(txId);
        if (txOpt.isPresent()) {
            Transaction tx = txOpt.get();
            tx.setStatus("CONFIRMED");
            tx.setConfirmations(1);
            tx.setConfirmedAt(new Date());
            transactionRepository.save(tx);
            System.out.println("Transaction confirmée : " + txId);
        }
    }
}