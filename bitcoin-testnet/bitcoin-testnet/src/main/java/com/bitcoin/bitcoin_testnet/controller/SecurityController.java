package com.bitcoin.bitcoin_testnet.controller;

import com.bitcoin.bitcoin_testnet.service.SecurityInsightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
public class SecurityController {

    private final SecurityInsightsService securityInsightsService;

    @GetMapping("/insights")
    public ResponseEntity<Map<String, Object>> getInsights() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(securityInsightsService.getInsights(userId));
    }

    @GetMapping("/score-history")
    public ResponseEntity<List<Map<String, Object>>> getScoreHistory() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(securityInsightsService.getScoreHistory(userId));
    }
}
