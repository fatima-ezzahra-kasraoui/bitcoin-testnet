package com.bitcoin.bitcoin_testnet.model;



import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@Document(collection = "wallets")
public class Wallet {
    @Id
    private String id;
    private String userId;
    private String address;
    private String publicKey;
    private String encryptedPrivateKey;
    private String label;
    private Date createdAt;
}