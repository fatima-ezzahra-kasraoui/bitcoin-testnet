package com.bitcoin.bitcoin_testnet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MessageRequest {

    @NotBlank(message = "L'adresse ne peut pas être vide")
    private String address;

    @NotBlank(message = "Le message ne peut pas être vide")
    @jakarta.validation.constraints.Size(min = 1, max = 500, message = "Le message doit avoir entre 1 et 500 caractères")
    private String message;
}