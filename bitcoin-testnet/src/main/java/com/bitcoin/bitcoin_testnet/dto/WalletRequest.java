package com.bitcoin.bitcoin_testnet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WalletRequest {

    @NotBlank(message = "L'userId ne peut pas être vide")
    private String userId;

    @NotBlank(message = "Le label ne peut pas être vide")
    @Size(min = 2, max = 50, message = "Le label doit avoir entre 2 et 50 caractères")
    private String label;
}