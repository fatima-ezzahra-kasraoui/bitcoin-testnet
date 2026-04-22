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
import org.springframework.stereotype.Service;
import org.bitcoinj.core.TransactionInput;

import java.util.Date;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final EncryptionService encryptionService;
    private final BitcoinService bitcoinService;
    private final KafkaProducerService kafkaProducerService;
    private final NetworkParameters params = TestNet3Params.get();

    public Transaction sendTransaction(String fromAddress, String toAddress, Long amount) {

        // 1. Récupère le wallet depuis MongoDB
        Wallet dbWallet = walletRepository.findByAddress(fromAddress)
                .orElseThrow(() -> new RuntimeException("Wallet not found: " + fromAddress));

        // 2. Crée la transaction en base avec statut PENDING
        Transaction tx = new Transaction();
        tx.setFromAddress(fromAddress);
        tx.setToAddress(toAddress);
        tx.setAmount(amount);
        tx.setStatus("PENDING");
        tx.setConfirmations(0);
        tx.setCreatedAt(new Date());

        // 3. Tente le broadcast sur TestNet via mempool.space API
        try {
            String rawTxHex = buildRawTransaction(dbWallet, toAddress, amount);
            if (rawTxHex != null) {
                String txId = broadcastTransaction(rawTxHex);
                if (txId != null) {
                    tx.setTxId(txId);
                    log.info("Transaction broadcastée : {}", txId);
                } else {
                    // Génère un txId temporaire si broadcast échoue
                    tx.setTxId("pending-" + System.currentTimeMillis());
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

        // 4. Sauvegarde en MongoDB
        Transaction saved = transactionRepository.save(tx);

        // 5. Publie sur Kafka pour traitement asynchrone
        kafkaProducerService.sendTransactionEvent(saved.getTxId());
        log.info("Event Kafka envoyé pour txId : {}", saved.getTxId());

        return saved;
    }

    // Construit la transaction Bitcoin brute (hex)
    private String buildRawTransaction(Wallet dbWallet, String toAddress, Long amountSatoshis) {
        try {
            // 1. Récupère la clé privée
            String privateKeyHex = encryptionService.decrypt(dbWallet.getEncryptedPrivateKey());
            ECKey key = ECKey.fromPrivate(HexFormat.of().parseHex(privateKeyHex));

            // 2. Récupère les UTXOs via mempool.space
            String utxosJson = httpGet("https://mempool.space/testnet/api/address/" + dbWallet.getAddress() + "/utxo");
            if (utxosJson == null || utxosJson.equals("[]")) {
                log.warn("Aucun UTXO disponible pour {}", dbWallet.getAddress());
                return null;
            }

            // 3. Parse le premier UTXO
            String txid = extractString(utxosJson, "txid");
            int vout = (int) extractLong(utxosJson, "vout");
            long value = extractLong(utxosJson, "value");

            if (value < amountSatoshis + 1000) {
                log.warn("Solde insuffisant : {} < {}", value, amountSatoshis + 1000);
                return null;
            }

            // 4. Construit la transaction
            org.bitcoinj.core.Transaction btcTx = new org.bitcoinj.core.Transaction(params);

            // Output destinataire
            Address toAddr = Address.fromString(params, toAddress);
            btcTx.addOutput(Coin.valueOf(amountSatoshis), toAddr);

            // Change (retour)
            long change = value - amountSatoshis - 1000;
            if (change > 0) {
                Address fromAddr = Address.fromString(params, dbWallet.getAddress());
                btcTx.addOutput(Coin.valueOf(change), fromAddr);
            }

            // Input
            Sha256Hash txHash = Sha256Hash.wrap(txid);
            btcTx.addInput(txHash, vout, new ScriptBuilder().build());

            // 5. Signature
            TransactionInput input = btcTx.getInput(0);

            // Calcule la signature
            Script outputScript = ScriptBuilder.createP2PKHOutputScript(key);
            TransactionSignature sig = btcTx.calculateSignature(0, key, outputScript, org.bitcoinj.core.Transaction.SigHash.ALL, false);

            // Crée le script d'entrée
            Script inputScript = new ScriptBuilder()
                    .data(sig.encodeToBitcoin())
                    .data(key.getPubKey())
                    .build();

            input.setScriptSig(inputScript);

            log.info("Transaction construite avec succès");
            return HexFormat.of().formatHex(btcTx.bitcoinSerialize());

        } catch (Exception e) {
            log.error("Erreur construction TX : {}", e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Broadcast la transaction sur TestNet via mempool.space
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

    public List<Transaction> getTransactions(String address) {
        return transactionRepository.findByFromAddressOrToAddress(address, address);
    }

    // Helpers JSON
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