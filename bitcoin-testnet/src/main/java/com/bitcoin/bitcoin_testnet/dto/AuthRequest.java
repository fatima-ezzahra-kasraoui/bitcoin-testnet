package com.bitcoin.bitcoin_testnet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthRequest {

    @NotBlank(message = "Le nom d'utilisateur ne peut pas être vide")
    @Size(min = 3, max = 30, message = "Le nom d'utilisateur doit avoir entre 3 et 30 caractères")
    private String username;

    @NotBlank(message = "Le mot de passe ne peut pas être vide")
    @Size(min = 6, max = 100, message = "Le mot de passe doit avoir au moins 6 caractères")
    private String password;
}