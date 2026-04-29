package com.bitcoin.bitcoin_testnet.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private String password;
    private String role;


     // MFA fields
    private String totpSecret;   // stored AES-256 encrypted
    private boolean mfaEnabled;  // false by default
}