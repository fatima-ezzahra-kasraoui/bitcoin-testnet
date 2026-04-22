package com.bitcoin.bitcoin_testnet.controller;

import com.bitcoin.bitcoin_testnet.dto.WalletRequest;
import com.bitcoin.bitcoin_testnet.model.Wallet;
import com.bitcoin.bitcoin_testnet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;


import java.util.List;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<Wallet> createWallet(@Valid @RequestBody WalletRequest request) {
        Wallet wallet = walletService.createWallet(request.getUserId(), request.getLabel());
        return ResponseEntity.ok(wallet);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Wallet>> getUserWallets(@PathVariable String userId) {
        List<Wallet> wallets = walletService.getUserWallets(userId);
        return ResponseEntity.ok(wallets);
    }
    @GetMapping("/{address}/balance")
    public ResponseEntity<Map<String, String>> getBalance(@PathVariable String address) {
        String balance = walletService.getBalance(address);
        return ResponseEntity.ok(Map.of("address", address, "balance", balance));
    }
    @PostMapping("/{address}/faucet")
    public ResponseEntity<Map<String, String>> requestFaucet(@PathVariable String address) {
        String result = walletService.requestFaucet(address);
        return ResponseEntity.ok(Map.of("address", address, "result", result));
    }
}