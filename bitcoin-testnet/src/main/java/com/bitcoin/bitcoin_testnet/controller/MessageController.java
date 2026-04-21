package com.bitcoin.bitcoin_testnet.controller;

import com.bitcoin.bitcoin_testnet.dto.MessageRequest;
import com.bitcoin.bitcoin_testnet.model.Message;
import com.bitcoin.bitcoin_testnet.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/sign")
    public ResponseEntity<Message> signMessage(@Valid @RequestBody MessageRequest request) {
        Message message = messageService.signMessage(
                request.getAddress(),
                request.getMessage()
        );
        return ResponseEntity.ok(message);
    }

    @GetMapping("/{address}")
    public ResponseEntity<List<Message>> getMessages(@PathVariable String address) {
        List<Message> messages = messageService.getMessages(address);
        return ResponseEntity.ok(messages);
    }
}