package com.bitcoin.bitcoin_testnet.service;

import com.bitcoin.bitcoin_testnet.model.Wallet;
import com.bitcoin.bitcoin_testnet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.bitcoinj.core.*;
import org.bitcoinj.params.TestNet3Params;
import org.springframework.stereotype.Service;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.MemoryBlockStore;
import org.bitcoinj.core.BlockChain;
import org.bitcoinj.core.PeerGroup;
import org.bitcoinj.core.Coin;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final EncryptionService encryptionService;
    private final NetworkParameters params = TestNet3Params.get();


    public Wallet createWallet(String userId, String label) {
        org.bitcoinj.wallet.Wallet btcWallet = org.bitcoinj.wallet.Wallet
                .createDeterministic(params, org.bitcoinj.script.Script.ScriptType.P2PKH);

        Address address = btcWallet.currentReceiveAddress();
        ECKey key = btcWallet.currentReceiveKey();

        Wallet wallet = new Wallet();
        wallet.setUserId(userId);
        wallet.setAddress(address.toString());
        wallet.setPublicKey(key.getPublicKeyAsHex());

        // Chiffrement AES-256 de la clé privée ✅
        String encryptedKey = encryptionService.encrypt(key.getPrivateKeyAsHex());
        wallet.setEncryptedPrivateKey(encryptedKey);

        wallet.setLabel(label);
        wallet.setCreatedAt(new Date());

        return walletRepository.save(wallet);
    }

    public List<Wallet> getUserWallets(String userId) {
        return walletRepository.findByUserId(userId);
    }
    public String getBalance(String address) {
        try {
            // 1. Connexion au réseau TestNet
            NetworkParameters params = TestNet3Params.get();
            BlockStore blockStore = new MemoryBlockStore(params);
            BlockChain chain = new BlockChain(params, blockStore);

            // 2. Crée un wallet BitcoinJ temporaire
            org.bitcoinj.wallet.Wallet tempWallet =
                    org.bitcoinj.wallet.Wallet.createDeterministic(
                            params,
                            org.bitcoinj.script.Script.ScriptType.P2PKH
                    );

            // 3. Connexion P2P au réseau
            PeerGroup peerGroup = new PeerGroup(params, chain);
            peerGroup.addWallet(tempWallet);
            peerGroup.start();
            peerGroup.downloadBlockChain();

            // 4. Récupère le solde
            Coin balance = tempWallet.getBalance();
            peerGroup.stop();

            return balance.toFriendlyString();

        } catch (Exception e) {
            return "0.00 BTC";
        }
    }
}