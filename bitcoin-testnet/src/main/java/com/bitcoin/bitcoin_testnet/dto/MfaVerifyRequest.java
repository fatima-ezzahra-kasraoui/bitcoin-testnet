package com.bitcoin.bitcoin_testnet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MfaVerifyRequest {

    @NotBlank(message = "Code cannot be empty")
    @Size(min = 6, max = 6, message = "Code must be exactly 6 digits")
    @Pattern(regexp = "\\d{6}", message = "Code must contain only digits")
    private String code;
}