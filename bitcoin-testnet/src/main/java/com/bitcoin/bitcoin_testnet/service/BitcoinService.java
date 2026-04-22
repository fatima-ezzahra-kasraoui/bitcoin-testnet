package com.bitcoin.bitcoin_testnet.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.bitcoinj.base.Address;
import org.bitcoinj.base.Network;
import org.bitcoinj.base.ScriptType;
import org.bitcoinj.core.*;
import org.bitcoinj.net.discovery.DnsDiscovery;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.store.SPVBlockStore;
import org.bitcoinj.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Service
public class BitcoinService {

    private static final Logger log = LoggerFactory.getLogger(BitcoinService.class);

    private final NetworkParameters params;
    private final Network network;
    private PeerGroup peerGroup;
    private BlockChain blockChain;
    private SPVBlockStore blockStore;

    public BitcoinService() {
        this.params = TestNet3Params.get();
        this.network = params.network();
        new Context(params);
    }

    @PostConstruct
    public void init() {
        try {
            log.info("=== Initialisation BitcoinJ TestNet ===");

            // Supprime l'ancien fichier SPV si corrompu
            File blockStoreFile = new File("testnet.spvchain");
            if (blockStoreFile.exists()) blockStoreFile.delete();

            blockStore = new SPVBlockStore(params, blockStoreFile);
            blockChain = new BlockChain(network, blockStore);

            peerGroup = new PeerGroup(network, blockChain);
            peerGroup.addPeerDiscovery(new DnsDiscovery(params));
            peerGroup.setMaxConnections(4);
            peerGroup.start();

            peerGroup.waitForPeers(1).get(30, TimeUnit.SECONDS);

            log.info("=== BitcoinJ connecté — {} peers ===",
                    peerGroup.getConnectedPeers().size());

        } catch (Exception e) {
            log.error("=== Erreur connexion BitcoinJ : {} ===", e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (peerGroup != null && peerGroup.isRunning()) peerGroup.stop();
            if (blockStore != null) blockStore.close();
            log.info("=== BitcoinJ arrêté proprement ===");
        } catch (Exception e) {
            log.error("Erreur shutdown BitcoinJ", e);
        }
    }

    public boolean isConnected() {
        return peerGroup != null
                && peerGroup.isRunning()
                && !peerGroup.getConnectedPeers().isEmpty();
    }

    public int getPeerCount() {
        if (peerGroup == null) return 0;
        return peerGroup.getConnectedPeers().size();
    }

    public String createWallet() {
        Wallet wallet = Wallet.createDeterministic(network, ScriptType.P2PKH);
        Address address = wallet.currentReceiveAddress();
        return address.toString();
    }

    public PeerGroup getPeerGroup() {
        return peerGroup;
    }

    public NetworkParameters getParams() {
        return params;
    }

    public Network getNetwork() {
        return network;
    }
}