package com.bitcoin.bitcoin_testnet.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Data
@Document(collection = "contacts")
public class Contact {
    @Id
    private String id;
    private String userId;
    private String label;
    private String address;
    private Date createdAt;
}