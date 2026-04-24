package com.bitcoin.bitcoin_testnet.controller;

import com.bitcoin.bitcoin_testnet.service.BitcoinService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/status")
@RequiredArgsConstructor
public class StatusController {

    private final BitcoinService bitcoinService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "connected", bitcoinService.isConnected(),
                "peers", bitcoinService.getPeerCount(),
                "network", "TestNet3"
        ));
    }
}