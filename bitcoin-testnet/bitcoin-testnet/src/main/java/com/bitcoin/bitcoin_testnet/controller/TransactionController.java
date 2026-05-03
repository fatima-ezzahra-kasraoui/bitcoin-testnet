package com.bitcoin.bitcoin_testnet.controller;

import com.bitcoin.bitcoin_testnet.dto.TransactionRequest;
import com.bitcoin.bitcoin_testnet.model.Transaction;
import com.bitcoin.bitcoin_testnet.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> sendTransaction(@Valid @RequestBody TransactionRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Map<String, Object> response = transactionService.sendTransaction(
                request.getFromAddress(),
                request.getToAddress(),
                request.getAmount(),
                userId
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{txId}/confirm")
    public ResponseEntity<?> confirmTransaction(@PathVariable String txId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            Map<String, Object> response = transactionService.confirmTransaction(txId, userId);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{txId}/cancel")
    public ResponseEntity<?> cancelTransaction(@PathVariable String txId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            Map<String, Object> response = transactionService.cancelTransaction(txId, userId);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pending-confirmation")
    public ResponseEntity<List<Transaction>> getPendingConfirmations() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(transactionService.getPendingConfirmations(userId));
    }

    @GetMapping("/{address}")
    public ResponseEntity<List<Transaction>> getTransactions(
            @PathVariable String address,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(transactionService.getFilteredTransactions(address, status, from, to, sort));
    }

    @GetMapping("/{address}/export")
    public ResponseEntity<byte[]> exportCsv(@PathVariable String address) {
        byte[] csv = transactionService.exportCsv(address);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transactions.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
