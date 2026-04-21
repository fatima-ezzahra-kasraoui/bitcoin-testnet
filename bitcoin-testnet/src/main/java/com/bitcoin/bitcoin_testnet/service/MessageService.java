package com.bitcoin.bitcoin_testnet.service;

import com.bitcoin.bitcoin_testnet.model.Message;
import com.bitcoin.bitcoin_testnet.model.Wallet;
import com.bitcoin.bitcoin_testnet.repository.MessageRepository;
import com.bitcoin.bitcoin_testnet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Utils;
import org.bitcoinj.params.TestNet3Params;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final WalletRepository walletRepository;

    public Message signMessage(String address, String messageText) {
        Wallet dbWallet = walletRepository.findByAddress(address)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        byte[] privateKeyBytes = Utils.HEX.decode(dbWallet.getEncryptedPrivateKey());
        ECKey key = ECKey.fromPrivate(privateKeyBytes);

        String signature = key.signMessage(messageText);

        Message message = new Message();
        message.setAddress(address);
        message.setMessage(messageText);
        message.setSignature(signature);
        message.setVerified(true);
        message.setCreatedAt(new Date());

        return messageRepository.save(message);
    }

    public List<Message> getMessages(String address) {
        return messageRepository.findByAddress(address);
    }
}