package com.bitcoin.bitcoin_testnet.service;

import com.bitcoin.bitcoin_testnet.model.Notification;
import com.bitcoin.bitcoin_testnet.model.Transaction;
import com.bitcoin.bitcoin_testnet.repository.NotificationRepository;
import com.bitcoin.bitcoin_testnet.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SecurityInsightsService {

    private final NotificationRepository notificationRepository;
    private final TransactionRepository transactionRepository;

    private static final Map<String, String> RULE_TIPS = new LinkedHashMap<>();
    static {
        RULE_TIPS.put("HIGH_AMOUNT", "Unusually large transaction detected. Verify the recipient.");
        RULE_TIPS.put("NEW_DESTINATION", "You sent to a new address. Always double-check it.");
        RULE_TIPS.put("RAPID_FIRE", "Multiple transactions in short time. Make sure this was you.");
        RULE_TIPS.put("OFF_HOURS", "Transaction outside your usual activity hours.");
        RULE_TIPS.put("ROUND_AMOUNT", "Round amounts are common in fraud. Stay vigilant.");
    }

    public Map<String, Object> getInsights(String userId) {
        Date thirtyDaysAgo = new Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000);
        SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy-MM-dd");

        List<Notification> alerts = notificationRepository
                .findByUserIdAndTypeAndCreatedAtAfter(userId, "SECURITY_ALERT", thirtyDaysAgo);
        List<Transaction> userTxs = transactionRepository.findByUserId(userId);

        int totalTransactions = userTxs.size();
        int flaggedTransactions = alerts.size();

        int avgRiskScore = 0;
        if (!alerts.isEmpty()) {
            avgRiskScore = (int) alerts.stream()
                    .filter(n -> n.getRiskScore() != null)
                    .mapToInt(Notification::getRiskScore)
                    .average()
                    .orElse(0);
        }

        String scoreLevel = avgRiskScore >= 75 ? "HIGH" : avgRiskScore >= 50 ? "MEDIUM" : "LOW";

        String lastAlertDate = alerts.stream()
                .filter(n -> n.getCreatedAt() != null)
                .max(Comparator.comparing(Notification::getCreatedAt))
                .map(n -> isoFmt.format(n.getCreatedAt()))
                .orElse(null);

        // Rules breakdown
        Map<String, Integer> rulesBreakdown = new LinkedHashMap<>();
        rulesBreakdown.put("HIGH_AMOUNT", 0);
        rulesBreakdown.put("NEW_DESTINATION", 0);
        rulesBreakdown.put("RAPID_FIRE", 0);
        rulesBreakdown.put("OFF_HOURS", 0);
        rulesBreakdown.put("ROUND_AMOUNT", 0);
        for (Notification alert : alerts) {
            if (alert.getTriggeredRules() != null) {
                for (String rule : alert.getTriggeredRules()) {
                    rulesBreakdown.merge(rule, 1, Integer::sum);
                }
            }
        }

        String mostFrequentRule = rulesBreakdown.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        // Alerts per day for last 7 days
        List<Map<String, Object>> alertsOverTime = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        for (int i = 6; i >= 0; i--) {
            cal.setTimeInMillis(System.currentTimeMillis());
            cal.add(Calendar.DAY_OF_MONTH, -i);
            String dateStr = dayFmt.format(cal.getTime());
            long count = alerts.stream()
                    .filter(n -> n.getCreatedAt() != null && dayFmt.format(n.getCreatedAt()).equals(dateStr))
                    .count();
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", dateStr);
            day.put("count", count);
            alertsOverTime.add(day);
        }

        // Top 3 risky transactions by riskScore
        List<Map<String, Object>> topRisky = alerts.stream()
                .filter(n -> n.getRiskScore() != null)
                .sorted(Comparator.comparingInt(Notification::getRiskScore).reversed())
                .limit(3)
                .map(n -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("txId", n.getTxId());
                    item.put("riskScore", n.getRiskScore());
                    item.put("triggeredRules", n.getTriggeredRules() != null ? n.getTriggeredRules() : List.of());
                    item.put("date", n.getCreatedAt() != null ? isoFmt.format(n.getCreatedAt()) : null);
                    return item;
                })
                .collect(Collectors.toList());

        // Tips only for rules that fired
        List<String> securityTips = RULE_TIPS.entrySet().stream()
                .filter(e -> rulesBreakdown.getOrDefault(e.getKey(), 0) > 0)
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalTransactions", totalTransactions);
        result.put("flaggedTransactions", flaggedTransactions);
        result.put("riskScore", avgRiskScore);
        result.put("scoreLevel", scoreLevel);
        result.put("lastAlertDate", lastAlertDate);
        result.put("mostFrequentRule", mostFrequentRule);
        result.put("rulesBreakdown", rulesBreakdown);
        result.put("alertsOverTime", alertsOverTime);
        result.put("topRiskyTransactions", topRisky);
        result.put("securityTips", securityTips);
        return result;
    }

    public List<Map<String, Object>> getScoreHistory(String userId) {
        return notificationRepository
                .findTop20ByUserIdAndTypeOrderByCreatedAtDesc(userId, "SECURITY_ALERT")
                .stream()
                .map(n -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("txId", n.getTxId());
                    item.put("riskScore", n.getRiskScore());
                    item.put("createdAt", n.getCreatedAt());
                    return item;
                })
                .collect(Collectors.toList());
    }
}
