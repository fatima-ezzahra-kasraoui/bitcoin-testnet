package com.bitcoin.bitcoin_testnet.service;

import com.bitcoin.bitcoin_testnet.model.Notification;
import com.bitcoin.bitcoin_testnet.model.Transaction;
import com.bitcoin.bitcoin_testnet.repository.NotificationRepository;
import com.bitcoin.bitcoin_testnet.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AnomalyDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetectionService.class);

    private final TransactionRepository transactionRepository;
    private final NotificationRepository notificationRepository;

    public static class AnalysisResult {
        private final int score;
        private final List<String> triggeredRules;

        public AnalysisResult(int score, List<String> triggeredRules) {
            this.score = score;
            this.triggeredRules = triggeredRules;
        }

        public int getScore() { return score; }
        public List<String> getTriggeredRules() { return triggeredRules; }
    }

    public AnalysisResult analyzeSync(Transaction tx, String userId) {
        return computeScore(tx);
    }

    @Async
    public void analyze(Transaction tx, String userId) {
        AnalysisResult result = computeScore(tx);
        int totalScore = result.getScore();
        List<String> triggeredRules = result.getTriggeredRules();

        if (totalScore >= 30) {
            Notification notif = new Notification();
            notif.setUserId(userId);
            notif.setType("SECURITY_ALERT");
            notif.setRiskScore(totalScore);
            notif.setTriggeredRules(triggeredRules);
            notif.setTxId(tx.getTxId());
            notif.setMessage("Suspicious transaction detected: " + tx.getTxId());
            notif.setRead(false);
            notif.setCreatedAt(new Date());
            notificationRepository.save(notif);
            log.info("Security alert saved for user {} — score {}, rules {}", userId, totalScore, triggeredRules);
        }
    }

    private AnalysisResult computeScore(Transaction tx) {
        int totalScore = 0;
        List<String> triggeredRules = new ArrayList<>();

        // Rule 1 — HIGH_AMOUNT (+30pts)
        List<Transaction> recentConfirmed = transactionRepository
                .findTop10ByFromAddressAndStatusOrderByCreatedAtDesc(tx.getFromAddress(), "CONFIRMED");
        if (!recentConfirmed.isEmpty()) {
            double avg = recentConfirmed.stream()
                    .filter(t -> t.getAmount() != null)
                    .mapToLong(Transaction::getAmount)
                    .average()
                    .orElse(0);
            if (avg > 0 && tx.getAmount() != null && tx.getAmount() > avg * 3) {
                totalScore += 30;
                triggeredRules.add("HIGH_AMOUNT");
            }
        }

        // Rule 2 — NEW_DESTINATION (+25pts)
        // tx is already saved; size == 1 means first time sending to this address
        List<Transaction> toAddressHistory = transactionRepository
                .findByFromAddressAndToAddress(tx.getFromAddress(), tx.getToAddress());
        if (toAddressHistory.size() <= 1) {
            totalScore += 25;
            triggeredRules.add("NEW_DESTINATION");
        }

        // Rule 3 — RAPID_FIRE (+20pts)
        Date twoMinAgo = new Date(System.currentTimeMillis() - 2 * 60 * 1000L);
        long recentCount = transactionRepository
                .countByFromAddressAndCreatedAtAfter(tx.getFromAddress(), twoMinAgo);
        if (recentCount >= 3) {
            totalScore += 20;
            triggeredRules.add("RAPID_FIRE");
        }

        // Rule 4 — OFF_HOURS (+15pts)
        List<Transaction> last20 = transactionRepository
                .findTop20ByFromAddressOrderByCreatedAtDesc(tx.getFromAddress());
        if (last20.size() >= 5) {
            Calendar cal = Calendar.getInstance();
            Map<Integer, Long> hourCounts = new HashMap<>();
            for (Transaction t : last20) {
                if (t.getCreatedAt() != null) {
                    cal.setTime(t.getCreatedAt());
                    int h = cal.get(Calendar.HOUR_OF_DAY);
                    hourCounts.merge(h, 1L, Long::sum);
                }
            }
            int mostCommonHour = hourCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(-1);
            if (mostCommonHour >= 0 && tx.getCreatedAt() != null) {
                cal.setTime(tx.getCreatedAt());
                int currentHour = cal.get(Calendar.HOUR_OF_DAY);
                int diff = Math.abs(currentHour - mostCommonHour);
                if (diff > 12) diff = 24 - diff;
                if (diff > 3) {
                    totalScore += 15;
                    triggeredRules.add("OFF_HOURS");
                }
            }
        }

        // Rule 5 — ROUND_AMOUNT (+10pts)
        if (tx.getAmount() != null && tx.getAmount() % 1000 == 0 && tx.getAmount() >= 10000) {
            totalScore += 10;
            triggeredRules.add("ROUND_AMOUNT");
        }

        return new AnalysisResult(totalScore, triggeredRules);
    }
}
