package com.bitcoin.bitcoin_testnet.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;
import java.util.List;

@Data
@Document(collection = "notifications")
public class Notification {
    @Id
    private String id;
    private String userId;
    private String message;
    private String txId;
    private boolean read = false;
    private Date createdAt;
    private String type = "TX_UPDATE";
    private Integer riskScore;
    private List<String> triggeredRules;
}
