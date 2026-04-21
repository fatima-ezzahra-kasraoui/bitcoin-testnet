package com.bitcoin.bitcoin_testnet.repository;

import com.bitcoin.bitcoin_testnet.model.Wallet;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface WalletRepository extends MongoRepository<Wallet, String> {
    List<Wallet> findByUserId(String userId);
    Optional<Wallet> findByAddress(String address);
}