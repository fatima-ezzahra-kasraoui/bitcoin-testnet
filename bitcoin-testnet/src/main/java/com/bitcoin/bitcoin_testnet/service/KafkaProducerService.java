package com.bitcoin.bitcoin_testnet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String TOPIC = "blockchain-transactions";

    public void sendTransactionEvent(String txId) {
        kafkaTemplate.send(TOPIC, txId);
        System.out.println("Event envoyé sur Kafka : " + txId);
    }
}