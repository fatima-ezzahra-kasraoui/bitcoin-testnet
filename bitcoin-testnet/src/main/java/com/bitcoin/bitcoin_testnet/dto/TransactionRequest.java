package com.bitcoin.bitcoin_testnet.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TransactionRequest {

    @NotBlank(message = "L'adresse source ne peut pas être vide")
    private String fromAddress;

    @NotBlank(message = "L'adresse destination ne peut pas être vide")
    private String toAddress;

    @Min(value = 1, message = "Le montant doit être supérieur à 0")
    private Long amount;
}