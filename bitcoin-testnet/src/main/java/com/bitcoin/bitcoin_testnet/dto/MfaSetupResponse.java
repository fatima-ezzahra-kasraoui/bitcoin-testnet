package com.bitcoin.bitcoin_testnet.dto;

import lombok.Data;

@Data
public class MfaSetupResponse {

    // The otpauth:// URI that gets encoded into the QR code
    // Google Authenticator scans this to set up the account
    private String qrCodeUrl;

    // The raw Base32 secret — shown as backup
    // User can type this manually into Google Authenticator if QR scan fails
    private String secret;
}