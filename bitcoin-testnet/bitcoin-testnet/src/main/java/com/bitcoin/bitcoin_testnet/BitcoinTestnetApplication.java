package com.bitcoin.bitcoin_testnet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BitcoinTestnetApplication {

	public static void main(String[] args) {
		SpringApplication.run(BitcoinTestnetApplication.class, args);
	}

}
