package com.bitcoin.bitcoin_testnet.dto;

import lombok.Data;

@Data
public class MfaSetupResponse {
    private String qrCodeUrl;
    private String secret;
}
