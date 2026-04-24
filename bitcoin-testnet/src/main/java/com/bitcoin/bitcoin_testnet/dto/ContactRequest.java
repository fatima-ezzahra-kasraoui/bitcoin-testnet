package com.bitcoin.bitcoin_testnet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContactRequest {
    @NotBlank(message = "Le label est requis")
    private String label;

    @NotBlank(message = "L'adresse est requise")
    private String address;
}