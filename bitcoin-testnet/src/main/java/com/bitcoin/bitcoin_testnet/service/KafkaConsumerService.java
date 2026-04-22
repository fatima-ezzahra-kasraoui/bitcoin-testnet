package com.bitcoin.bitcoin_testnet.service;

import com.bitcoin.bitcoin_testnet.model.Transaction;
import com.bitcoin.bitcoin_testnet.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@EnableScheduling
public class KafkaConsumerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);
    private final TransactionRepository transactionRepository;

    // 1. Vérification au démarrage
    @EventListener(ApplicationReadyEvent.class)
    public void checkPendingTransactionsOnStartup() {
        log.info("🔍 Vérification des transactions PENDING au démarrage...");
        updateAllPendingTransactions();
    }

    // 2. Vérification automatique toutes les 30 secondes
    @Scheduled(fixedDelay = 30000)
    public void scheduledCheckConfirmations() {
        log.info("🔄 Vérification automatique des confirmations...");
        updateAllPendingTransactions();
    }

    // 3. Vérification via Kafka
    @KafkaListener(topics = "blockchain-transactions", groupId = "bitcoin-group")
    public void listen(String txId) {
        log.info("📨 Event reçu de Kafka : {}", txId);

        if (txId.startsWith("pending-")) {
            log.warn("⏭️ TxId temporaire ignoré : {}", txId);
            return;
        }

        Optional<Transaction> txOpt = transactionRepository.findByTxId(txId);
        if (txOpt.isPresent()) {
            updateSingleTransaction(txOpt.get());
        }
    }

    // Vérifie toutes les transactions PENDING
    private void updateAllPendingTransactions() {
        List<Transaction> pendingTxs = transactionRepository.findByStatus("PENDING");

        if (pendingTxs.isEmpty()) {
            log.debug("📊 Aucune transaction PENDING");
            return;
        }

        log.info("📊 {} transaction(s) PENDING à vérifier", pendingTxs.size());

        for (Transaction tx : pendingTxs) {
            if (tx.getTxId() != null && !tx.getTxId().startsWith("pending-")) {
                updateSingleTransaction(tx);
            }
        }
    }

    // Vérifie et met à jour une transaction
    private void updateSingleTransaction(Transaction tx) {
        String txId = tx.getTxId();

        try {
            String url = "https://mempool.space/testnet/api/tx/" + txId;
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String json = response.toString();

                // 1. Extraire le block_height
                int blockHeight = 0;
                String search = "\"block_height\":";
                int idx = json.indexOf(search);
                if (idx != -1) {
                    int start = idx + search.length();
                    int end = start;
                    while (end < json.length() && Character.isDigit(json.charAt(end))) {
                        end++;
                    }
                    blockHeight = Integer.parseInt(json.substring(start, end));
                }

                // 2. Récupérer la hauteur actuelle
                int currentHeight = getCurrentBlockHeight();

                // 3. Calculer les confirmations
                int confirmations = 0;
                if (blockHeight > 0 && currentHeight > 0) {
                    confirmations = currentHeight - blockHeight + 1;
                }

                log.info("📊 Transaction {} : {} confirmations (block={}, current={})", txId, confirmations, blockHeight, currentHeight);

                // 4. Mettre à jour si changé
                if (confirmations != tx.getConfirmations()) {
                    tx.setConfirmations(confirmations);
                    if (confirmations >= 3) {
                        tx.setStatus("CONFIRMED");
                        tx.setConfirmedAt(new Date());
                    }
                    transactionRepository.save(tx);
                    log.info("✅ Transaction mise à jour : {} ({} confirmations)", txId, confirmations);
                }
            }
        } catch (Exception e) {
            log.error("❌ Erreur vérification {} : {}", txId, e.getMessage());
        }
    }
    private int getCurrentBlockHeight() {
        try {
            URL url = new URL("https://mempool.space/testnet/api/blocks/tip/height");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String height = reader.readLine();
                reader.close();
                return Integer.parseInt(height);
            }
        } catch (Exception e) {
            log.error("Erreur getCurrentBlockHeight: {}", e.getMessage());
        }
        return 0;
    }

    // Extrait le nombre de confirmations du JSON
    private int extractConfirmations(String json) {
        try {
            String search = "\"confirmations\":";
            int idx = json.indexOf(search);
            if (idx == -1) return 0;

            int start = idx + search.length();
            int end = start;
            while (end < json.length() && Character.isDigit(json.charAt(end))) {
                end++;
            }

            if (start < end) {
                return Integer.parseInt(json.substring(start, end));
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
}