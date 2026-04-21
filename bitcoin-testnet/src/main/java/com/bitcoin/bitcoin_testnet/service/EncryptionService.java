package com.bitcoin.bitcoin_testnet.service;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EncryptionService {

    // Clé secrète de 32 caractères (256 bits)
    private final String SECRET_KEY = "bitcoin-secret-key-32-chars-ok!!";

    public String encrypt(String plainText) {
        try {
            // 1. Génère un vecteur d'initialisation aléatoire (IV)
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // 2. Prépare la clé AES
            SecretKeySpec keySpec = new SecretKeySpec(
                    SECRET_KEY.getBytes(), "AES"
            );

            // 3. Chiffre le texte
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes());

            // 4. Retourne IV + texte chiffré en Base64
            String ivBase64 = Base64.getEncoder().encodeToString(iv);
            String encryptedBase64 = Base64.getEncoder().encodeToString(encrypted);
            return "AES256:" + ivBase64 + ":" + encryptedBase64;

        } catch (Exception e) {
            throw new RuntimeException("Erreur chiffrement", e);
        }
    }

    public String decrypt(String encryptedText) {
        try {
            // 1. Sépare les parties AES256:iv:ciphertext
            String[] parts = encryptedText.split(":");
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] encrypted = Base64.getDecoder().decode(parts[2]);

            // 2. Prépare la clé AES
            SecretKeySpec keySpec = new SecretKeySpec(
                    SECRET_KEY.getBytes(), "AES"
            );

            // 3. Déchiffre
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));
            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted);

        } catch (Exception e) {
            throw new RuntimeException("Erreur déchiffrement", e);
        }
    }
}