package com.bitcoin.bitcoin_testnet.repository;

import com.bitcoin.bitcoin_testnet.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface MessageRepository extends MongoRepository<Message, String> {
    List<Message> findByAddress(String address);
}