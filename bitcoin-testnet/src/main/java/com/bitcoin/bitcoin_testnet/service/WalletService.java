package com.bitcoin.bitcoin_testnet.service;

import com.bitcoin.bitcoin_testnet.model.Wallet;
import com.bitcoin.bitcoin_testnet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.bitcoinj.base.Address;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.ECKey;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.DeterministicSeed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository walletRepository;
    private final EncryptionService encryptionService;
    private final BitcoinService bitcoinService;
    private final NetworkParameters params = TestNet3Params.get();

    public Wallet createWallet(String userId, String label) {
        org.bitcoinj.wallet.Wallet btcWallet = org.bitcoinj.wallet.Wallet
                .createDeterministic(params, org.bitcoinj.base.ScriptType.P2PKH);
        Address address = btcWallet.currentReceiveAddress();
        ECKey key = btcWallet.currentReceiveKey();

        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setAddress(address.toString());
        wallet.setPublicKey(key.getPublicKeyAsHex());
        wallet.setEncryptedPrivateKey(encryptionService.encrypt(key.getPrivateKeyAsHex()));
        wallet.setLabel(label);
        wallet.setCreatedAt(new Date());

        return walletRepository.save(wallet);
    }

    public List<Wallet> getUserWallets(String userId) {
        return walletRepository.findByUserId(userId);
    }

    public String getBalance(String address) {
        try {
            String url = "https://mempool.space/testnet/api/address/" + address;
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                    new java.net.URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                java.util.Scanner sc = new java.util.Scanner(conn.getInputStream());
                StringBuilder sb = new StringBuilder();
                while (sc.hasNext()) sb.append(sc.nextLine());
                sc.close();

                String json = sb.toString();
                long confirmed = extractLong(json, "funded_txo_sum")
                        - extractLong(json, "spent_txo_sum");

                double btc = confirmed / 100_000_000.0;
                log.info("Balance pour {} : {} BTC", address, btc);
                return String.format("%.8f BTC", btc);
            }
        } catch (Exception e) {
            log.warn("API mempool.space indisponible : {}", e.getMessage());
        }

        if (!bitcoinService.isConnected()) {
            log.warn("BitcoinJ non connecté — balance indisponible");
            return "Réseau indisponible — réessayez dans quelques secondes";
        }

        return "0.00000000 BTC";
    }

    @PostConstruct
    public void initMasterWallet() {
        try {
            String seedPhrase = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about";
            DeterministicSeed seed = new DeterministicSeed(seedPhrase, null, "", 0);
            org.bitcoinj.wallet.Wallet masterWallet = org.bitcoinj.wallet.Wallet
                    .fromSeed(params, seed, org.bitcoinj.base.ScriptType.P2PKH);
            String masterAddress = masterWallet.currentReceiveAddress().toString();
            log.info("=== WALLET MASTER CHARGÉ ===");
            log.info("Adresse : {}", masterAddress);
            log.info("Solde : {}", masterWallet.getBalance().toFriendlyString());
        } catch (Exception e) {
            log.error("Erreur chargement wallet master: {}", e.getMessage());
        }
    }

    public String requestFaucet(String address) {
        try {
            // API officielle mempool.space testnet faucet
            String url = "https://mempool.space/testnet/faucet";
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                    new java.net.URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            String body = "{\"sats\":10000,\"address\":\"" + address + "\"}";
            conn.getOutputStream().write(body.getBytes());

            int code = conn.getResponseCode();
            java.util.Scanner sc = new java.util.Scanner(
                    code == 200 ? conn.getInputStream() : conn.getErrorStream());
            StringBuilder sb = new StringBuilder();
            while (sc.hasNext()) sb.append(sc.nextLine());
            String response = sb.toString();

            log.info("Mempool faucet réponse ({}) : {}", code, response);

            if (code == 200) {
                return "✅ " + response;
            } else {
                return "⚠️ Faucet réponse : " + response;
            }
        } catch (Exception e) {
            log.error("Erreur faucet : {}", e.getMessage());
            return "❌ Erreur : " + e.getMessage();
        }
    }

    private long extractLong(String json, String key) {
        try {
            int idx = json.indexOf("\"" + key + "\":");
            if (idx == -1) return 0;
            int start = idx + key.length() + 3;
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            return Long.parseLong(json.substring(start, end).trim());
        } catch (Exception e) {
            return 0;
        }
    }
    public Wallet updateLabel(String address, String newLabel) {
        Wallet wallet = walletRepository.findByAddress(address)
                .orElseThrow(() -> new RuntimeException("Wallet not found: " + address));
        wallet.setLabel(newLabel);
        return walletRepository.save(wallet);
    }
    public void deleteWallet(String address) {
        Wallet wallet = walletRepository.findByAddress(address)
                .orElseThrow(() -> new RuntimeException("Wallet not found: " + address));

        // Vérifie le solde
        String balanceStr = getBalance(address);
        double balance = Double.parseDouble(balanceStr.replace(" BTC", "").replace(",", "."));

        if (balance > 0) {
            throw new RuntimeException("Impossible de supprimer : le wallet a un solde de " + balanceStr);
        }

        walletRepository.delete(wallet);
    }


}