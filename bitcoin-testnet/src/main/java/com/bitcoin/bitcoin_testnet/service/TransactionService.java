package com.bitcoin.bitcoin_testnet.service;

import com.bitcoin.bitcoin_testnet.model.Transaction;
import com.bitcoin.bitcoin_testnet.model.Wallet;
import com.bitcoin.bitcoin_testnet.repository.TransactionRepository;
import com.bitcoin.bitcoin_testnet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.bitcoinj.core.*;
import org.bitcoinj.params.TestNet3Params;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final NetworkParameters params = TestNet3Params.get();

    public Transaction sendTransaction(String fromAddress, String toAddress, Long amount) {
        Wallet dbWallet = walletRepository.findByAddress(fromAddress)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        Transaction tx = new Transaction();
        tx.setFromAddress(fromAddress);
        tx.setToAddress(toAddress);
        tx.setAmount(amount);
        tx.setStatus("PENDING");
        tx.setConfirmations(0);
        tx.setCreatedAt(new Date());

        return transactionRepository.save(tx);
    }

    public List<Transaction> getTransactions(String address) {
        return transactionRepository.findByFromAddressOrToAddress(address, address);
    }
}