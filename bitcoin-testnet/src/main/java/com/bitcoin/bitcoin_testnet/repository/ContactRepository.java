package com.bitcoin.bitcoin_testnet.repository;

import com.bitcoin.bitcoin_testnet.model.Contact;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ContactRepository extends MongoRepository<Contact, String> {
    List<Contact> findByUserId(String userId);
    void deleteByIdAndUserId(String id, String userId);
}