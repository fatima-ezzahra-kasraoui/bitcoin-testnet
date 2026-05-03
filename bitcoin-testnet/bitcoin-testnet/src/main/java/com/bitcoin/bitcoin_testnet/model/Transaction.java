package com.bitcoin.bitcoin_testnet.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
@Document(collection = "transactions")
public class Transaction {
    @Id
    private String id;
    private String txId;
    private String userId;
    private String fromAddress;
    private String toAddress;
    private Long amount;
    private String status; // PENDING, CONFIRMED, FAILED, AWAITING_CONFIRMATION
    private Integer confirmations;
    private Date createdAt;
    private Date confirmedAt;
    private boolean requiresConfirmation;
    private LocalDateTime confirmationExpiresAt;
    private Integer riskScore;
    private List<String> triggeredRules;
}