package com.bitcoin.bitcoin_testnet.service;

import com.bitcoin.bitcoin_testnet.model.Message;
import com.bitcoin.bitcoin_testnet.model.Wallet;
import com.bitcoin.bitcoin_testnet.repository.MessageRepository;
import com.bitcoin.bitcoin_testnet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.bitcoinj.crypto.ECKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final MessageRepository messageRepository;
    private final WalletRepository walletRepository;
    private final EncryptionService encryptionService;

    public Message signMessage(String address, String messageText) {
        try {
            // 1. Récupérer le wallet
            Wallet dbWallet = walletRepository.findByAddress(address)
                    .orElseThrow(() -> new RuntimeException("Wallet not found: " + address));

            // 2. Récupérer la clé privée chiffrée
            String encryptedKey = dbWallet.getEncryptedPrivateKey();
            log.info("Clé chiffrée brute: {}", encryptedKey);

            // 3. Déchiffrer la clé
            String decryptedKey = encryptionService.decrypt(encryptedKey);
            log.info("Clé déchiffrée brute: {}", decryptedKey);

            // 4. Extraire uniquement la partie hexadécimale (supprimer "AES256:" et autres préfixes)
            String hexKey = extractHexKey(decryptedKey);
            log.info("Clé hex nettoyée: {}", hexKey);

            // 5. Vérifier que c'est bien de l'hex valide
            if (!isValidHex(hexKey)) {
                throw new RuntimeException("La clé déchiffrée n'est pas en hexadécimal valide: " + hexKey);
            }

            // 6. Convertir en bytes et créer ECKey
            byte[] privateKeyBytes = hexStringToByteArray(hexKey);
            ECKey key = ECKey.fromPrivate(privateKeyBytes);

            // 7. Signer le message
            String signature = key.signMessage(messageText);
            log.info("✅ Message signé avec succès");

            // 8. Sauvegarder
            Message message = new Message();
            message.setAddress(address);
            message.setMessage(messageText);
            message.setSignature(signature);
            message.setVerified(true);
            message.setCreatedAt(new Date());

            return messageRepository.save(message);

        } catch (Exception e) {
            log.error("❌ Erreur signature: {}", e.getMessage());
            throw new RuntimeException("Erreur signature: " + e.getMessage());
        }
    }

    // Extrait la partie hexadécimale d'une chaîne
    private String extractHexKey(String input) {
        // Supprime "AES256:" au début si présent
        String cleaned = input.replaceAll("^AES256:", "");

        // Supprime tout ce qui n'est pas hexadécimal (0-9, a-f, A-F)
        String hexOnly = cleaned.replaceAll("[^0-9a-fA-F]", "");

        return hexOnly;
    }

    // Vérifie si une chaîne est valide hexadécimal
    private boolean isValidHex(String str) {
        if (str == null || str.isEmpty()) return false;
        return str.matches("^[0-9a-fA-F]+$");
    }

    // Convertit une chaîne hex en byte array
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public List<Message> getMessages(String address) {
        return messageRepository.findByAddress(address);
    }
}