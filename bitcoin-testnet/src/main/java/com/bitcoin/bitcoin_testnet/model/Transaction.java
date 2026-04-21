package com.bitcoin.bitcoin_testnet.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@Document(collection = "transactions")
public class Transaction {
    @Id
    private String id;
    private String txId;
    private String fromAddress;
    private String toAddress;
    private Long amount;
    private String status; // PENDING, CONFIRMED, FAILED
    private Integer confirmations;
    private Date createdAt;
    private Date confirmedAt;
}