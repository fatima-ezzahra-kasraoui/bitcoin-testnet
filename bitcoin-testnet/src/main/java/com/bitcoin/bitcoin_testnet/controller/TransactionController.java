package com.bitcoin.bitcoin_testnet.controller;

import com.bitcoin.bitcoin_testnet.dto.TransactionRequest;
import com.bitcoin.bitcoin_testnet.model.Transaction;
import com.bitcoin.bitcoin_testnet.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<Transaction> sendTransaction(@Valid @RequestBody TransactionRequest request) {
        Transaction tx = transactionService.sendTransaction(
                request.getFromAddress(),
                request.getToAddress(),
                request.getAmount()
        );
        return ResponseEntity.ok(tx);
    }

    @GetMapping("/{address}")
    public ResponseEntity<List<Transaction>> getTransactions(@PathVariable String address) {
        List<Transaction> transactions = transactionService.getTransactions(address);
        return ResponseEntity.ok(transactions);
    }
}