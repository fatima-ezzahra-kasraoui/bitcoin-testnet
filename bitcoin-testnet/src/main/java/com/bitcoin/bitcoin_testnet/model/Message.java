package com.bitcoin.bitcoin_testnet.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@Document(collection = "messages")
public class Message {
    @Id
    private String id;
    private String address;
    private String message;
    private String signature;
    private Boolean verified;
    private Date createdAt;
}