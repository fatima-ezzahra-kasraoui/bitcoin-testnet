package com.bitcoin.bitcoin_testnet.service;

import org.bitcoinj.core.*;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.wallet.Wallet;
import org.springframework.stereotype.Service;

@Service
public class BitcoinService {

    private final NetworkParameters params;

    public BitcoinService() {
        this.params = TestNet3Params.get();
    }

    public String createWallet() {
        Wallet wallet = Wallet.createDeterministic(params, org.bitcoinj.script.Script.ScriptType.P2PKH);
        Address address = wallet.currentReceiveAddress();
        return address.toString();
    }
}