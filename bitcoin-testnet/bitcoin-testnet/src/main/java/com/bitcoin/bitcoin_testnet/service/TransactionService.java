package com.bitcoin.bitcoin_testnet.service;

import com.bitcoin.bitcoin_testnet.model.Transaction;
import com.bitcoin.bitcoin_testnet.model.Wallet;
import com.bitcoin.bitcoin_testnet.repository.TransactionRepository;
import com.bitcoin.bitcoin_testnet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.bitcoinj.base.Address;
import org.bitcoinj.base.Coin;
import org.bitcoinj.base.Sha256Hash;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.ECKey;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.bitcoinj.core.TransactionInput;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final EncryptionService encryptionService;
    private final BitcoinService bitcoinService;
    private final KafkaProducerService kafkaProducerService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final MongoTemplate mongoTemplate;
    private final NetworkParameters params = TestNet3Params.get();

    public Map<String, Object> sendTransaction(String fromAddress, String toAddress, Long amount, String userId) {

        Wallet dbWallet = walletRepository.findByAddress(fromAddress)
                .orElseThrow(() -> new RuntimeException("Wallet not found: " + fromAddress));

        Transaction tx = new Transaction();
        tx.setUserId(userId);
        tx.setFromAddress(fromAddress);
        tx.setToAddress(toAddress);
        tx.setAmount(amount);
        tx.setStatus("PENDING");
        tx.setConfirmations(0);
        tx.setCreatedAt(new Date());
        tx.setRequiresConfirmation(false);

        // Save first so analyzeSync can query DB for pattern detection
        tx = transactionRepository.save(tx);

        // Synchronous risk check before broadcasting
        AnomalyDetectionService.AnalysisResult analysisResult = anomalyDetectionService.analyzeSync(tx, userId);

        if (analysisResult.getScore() >= 50) {
            tx.setStatus("AWAITING_CONFIRMATION");
            tx.setRequiresConfirmation(true);
            tx.setConfirmationExpiresAt(LocalDateTime.now().plusMinutes(10));
            tx.setRiskScore(analysisResult.getScore());
            tx.setTriggeredRules(analysisResult.getTriggeredRules());
            transactionRepository.save(tx);
            log.info("Transaction held for confirmation — id={}, score={}", tx.getId(), analysisResult.getScore());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "AWAITING_CONFIRMATION");
            response.put("riskScore", analysisResult.getScore());
            response.put("txId", tx.getId());
            response.put("message", "High risk transaction. Please confirm.");
            response.put("triggeredRules", analysisResult.getTriggeredRules());
            response.put("toAddress", toAddress);
            response.put("amount", amount);
            response.put("expiresAt", tx.getConfirmationExpiresAt()
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            return response;
        }

        // Low risk — broadcast normally
        try {
            String rawTxHex = buildRawTransaction(dbWallet, toAddress, amount);
            if (rawTxHex != null) {
                String txId = broadcastTransaction(rawTxHex);
                tx.setTxId(txId != null ? txId : "pending-" + System.currentTimeMillis());
                if (txId != null) {
                    log.info("Transaction broadcastée : {}", txId);
                } else {
                    log.warn("Broadcast échoué — txId temporaire assigné");
                }
            } else {
                tx.setTxId("pending-" + System.currentTimeMillis());
                log.warn("Construction tx échouée — txId temporaire");
            }
        } catch (Exception e) {
            tx.setTxId("pending-" + System.currentTimeMillis());
            log.error("Erreur broadcast : {}", e.getMessage());
        }

        Transaction saved = transactionRepository.save(tx);
        anomalyDetectionService.analyze(saved, userId);
        kafkaProducerService.sendTransactionEvent(saved.getTxId());
        log.info("Event Kafka envoyé pour txId : {}", saved.getTxId());

        return buildTransactionMap(saved);
    }

    public Map<String, Object> confirmTransaction(String docId, String userId) {
        Transaction tx = transactionRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!tx.getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized");
        }
        if (!"AWAITING_CONFIRMATION".equals(tx.getStatus())) {
            throw new IllegalStateException("Transaction is not awaiting confirmation");
        }
        if (tx.getConfirmationExpiresAt() == null || LocalDateTime.now().isAfter(tx.getConfirmationExpiresAt())) {
            throw new IllegalStateException("Confirmation window has expired");
        }

        Wallet dbWallet = walletRepository.findByAddress(tx.getFromAddress())
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        try {
            String rawTxHex = buildRawTransaction(dbWallet, tx.getToAddress(), tx.getAmount());
            if (rawTxHex != null) {
                String txId = broadcastTransaction(rawTxHex);
                tx.setTxId(txId != null ? txId : "pending-" + System.currentTimeMillis());
            } else {
                tx.setTxId("pending-" + System.currentTimeMillis());
            }
        } catch (Exception e) {
            tx.setTxId("pending-" + System.currentTimeMillis());
            log.error("Broadcast error after user confirm: {}", e.getMessage());
        }

        tx.setStatus("PENDING");
        tx.setRequiresConfirmation(false);
        Transaction saved = transactionRepository.save(tx);
        anomalyDetectionService.analyze(saved, userId);
        kafkaProducerService.sendTransactionEvent(saved.getTxId());
        log.info("User confirmed high-risk transaction — docId={}, txId={}", docId, saved.getTxId());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "confirmed");
        return response;
    }

    public Map<String, Object> cancelTransaction(String docId, String userId) {
        Transaction tx = transactionRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!tx.getUserId().equals(userId)) {
            throw new SecurityException("Unauthorized");
        }

        tx.setStatus("FAILED");
        transactionRepository.save(tx);
        log.info("Transaction cancelled by user — docId={}", docId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "cancelled");
        return response;
    }

    public List<Transaction> getPendingConfirmations(String userId) {
        return transactionRepository.findByUserIdAndStatusAndConfirmationExpiresAtAfter(
                userId, "AWAITING_CONFIRMATION", LocalDateTime.now());
    }

    private Map<String, Object> buildTransactionMap(Transaction tx) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", tx.getId());
        map.put("txId", tx.getTxId());
        map.put("userId", tx.getUserId());
        map.put("fromAddress", tx.getFromAddress());
        map.put("toAddress", tx.getToAddress());
        map.put("amount", tx.getAmount());
        map.put("status", tx.getStatus());
        map.put("confirmations", tx.getConfirmations());
        map.put("createdAt", tx.getCreatedAt());
        map.put("confirmedAt", tx.getConfirmedAt());
        map.put("requiresConfirmation", tx.isRequiresConfirmation());
        return map;
    }

    public List<Transaction> getFilteredTransactions(String address, String status,
                                                      String from, String to, String sort) {
        Query query = new Query();

        query.addCriteria(new Criteria().orOperator(
                Criteria.where("fromAddress").is(address),
                Criteria.where("toAddress").is(address)
        ));

        if (status != null && !status.isBlank()) {
            query.addCriteria(Criteria.where("status").is(status));
        }

        if (from != null && !from.isBlank() && to != null && !to.isBlank()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date toDate = new Date(sdf.parse(to).getTime() + 86400000L);
                query.addCriteria(Criteria.where("createdAt").gte(sdf.parse(from)).lte(toDate));
            } catch (Exception ignored) {}
        } else if (from != null && !from.isBlank()) {
            try {
                query.addCriteria(Criteria.where("createdAt").gte(new SimpleDateFormat("yyyy-MM-dd").parse(from)));
            } catch (Exception ignored) {}
        } else if (to != null && !to.isBlank()) {
            try {
                Date toDate = new Date(new SimpleDateFormat("yyyy-MM-dd").parse(to).getTime() + 86400000L);
                query.addCriteria(Criteria.where("createdAt").lte(toDate));
            } catch (Exception ignored) {}
        }

        if ("amount".equals(sort)) {
            query.with(Sort.by(Sort.Direction.DESC, "amount"));
        } else if ("date-asc".equals(sort)) {
            query.with(Sort.by(Sort.Direction.ASC, "createdAt"));
        } else {
            query.with(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        return mongoTemplate.find(query, Transaction.class);
    }

    public byte[] exportCsv(String address) {
        List<Transaction> txs = transactionRepository.findByFromAddressOrToAddress(address, address);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        StringBuilder sb = new StringBuilder();
        sb.append("txId,fromAddress,toAddress,amount,status,createdAt,confirmedAt\n");
        for (Transaction tx : txs) {
            sb.append(csvSafe(tx.getTxId())).append(",");
            sb.append(csvSafe(tx.getFromAddress())).append(",");
            sb.append(csvSafe(tx.getToAddress())).append(",");
            sb.append(tx.getAmount() != null ? tx.getAmount() : "").append(",");
            sb.append(csvSafe(tx.getStatus())).append(",");
            sb.append(tx.getCreatedAt() != null ? sdf.format(tx.getCreatedAt()) : "").append(",");
            sb.append(tx.getConfirmedAt() != null ? sdf.format(tx.getConfirmedAt()) : "").append("\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csvSafe(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    private String buildRawTransaction(Wallet dbWallet, String toAddress, Long amountSatoshis) {
        try {
            String privateKeyHex = encryptionService.decrypt(dbWallet.getEncryptedPrivateKey());
            ECKey key = ECKey.fromPrivate(HexFormat.of().parseHex(privateKeyHex));

            String utxosJson = httpGet("https://mempool.space/testnet/api/address/" + dbWallet.getAddress() + "/utxo");
            if (utxosJson == null || utxosJson.equals("[]")) {
                log.warn("Aucun UTXO disponible pour {}", dbWallet.getAddress());
                return null;
            }

            String txid = extractString(utxosJson, "txid");
            int vout = (int) extractLong(utxosJson, "vout");
            long value = extractLong(utxosJson, "value");

            if (value < amountSatoshis + 1000) {
                log.warn("Solde insuffisant : {} < {}", value, amountSatoshis + 1000);
                return null;
            }

            org.bitcoinj.core.Transaction btcTx = new org.bitcoinj.core.Transaction(params);

            Address toAddr = Address.fromString(params, toAddress);
            btcTx.addOutput(Coin.valueOf(amountSatoshis), toAddr);

            long change = value - amountSatoshis - 1000;
            if (change > 0) {
                Address fromAddr = Address.fromString(params, dbWallet.getAddress());
                btcTx.addOutput(Coin.valueOf(change), fromAddr);
            }

            Sha256Hash txHash = Sha256Hash.wrap(txid);
            btcTx.addInput(txHash, vout, new ScriptBuilder().build());

            TransactionInput input = btcTx.getInput(0);
            Script outputScript = ScriptBuilder.createP2PKHOutputScript(key);
            TransactionSignature sig = btcTx.calculateSignature(0, key, outputScript,
                    org.bitcoinj.core.Transaction.SigHash.ALL, false);

            Script inputScript = new ScriptBuilder()
                    .data(sig.encodeToBitcoin())
                    .data(key.getPubKey())
                    .build();
            input.setScriptSig(inputScript);

            log.info("Transaction construite avec succès");
            return HexFormat.of().formatHex(btcTx.bitcoinSerialize());

        } catch (Exception e) {
            log.error("Erreur construction TX : {}", e.getMessage());
            return null;
        }
    }

    private String broadcastTransaction(String rawTxHex) {
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                    new java.net.URL("https://mempool.space/testnet/api/tx").openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Content-Type", "text/plain");
            conn.getOutputStream().write(rawTxHex.getBytes());

            if (conn.getResponseCode() == 200) {
                java.util.Scanner sc = new java.util.Scanner(conn.getInputStream());
                StringBuilder sb = new StringBuilder();
                while (sc.hasNext()) sb.append(sc.nextLine());
                return sb.toString().trim();
            } else {
                java.util.Scanner sc = new java.util.Scanner(conn.getErrorStream());
                StringBuilder sb = new StringBuilder();
                while (sc.hasNext()) sb.append(sc.nextLine());
                log.error("Broadcast error : {}", sb.toString());
                return null;
            }
        } catch (Exception e) {
            log.error("Erreur HTTP broadcast : {}", e.getMessage());
            return null;
        }
    }

    private String httpGet(String url) {
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                    new java.net.URL(url).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            if (conn.getResponseCode() == 200) {
                java.util.Scanner sc = new java.util.Scanner(conn.getInputStream());
                StringBuilder sb = new StringBuilder();
                while (sc.hasNext()) sb.append(sc.nextLine());
                return sb.toString();
            }
        } catch (Exception e) {
            log.error("HTTP GET error : {}", e.getMessage());
        }
        return null;
    }

    private String extractString(String json, String key) {
        try {
            int idx = json.indexOf("\"" + key + "\":\"");
            if (idx == -1) return null;
            int start = idx + key.length() + 4;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } catch (Exception e) { return null; }
    }

    private long extractLong(String json, String key) {
        try {
            int idx = json.indexOf("\"" + key + "\":");
            if (idx == -1) return 0;
            int start = idx + key.length() + 3;
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            return Long.parseLong(json.substring(start, end).trim());
        } catch (Exception e) { return 0; }
    }
}
