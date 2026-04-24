package com.bitcoin.bitcoin_testnet.controller;

import com.bitcoin.bitcoin_testnet.dto.ContactRequest;
import com.bitcoin.bitcoin_testnet.model.Contact;
import com.bitcoin.bitcoin_testnet.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    public ResponseEntity<Contact> addContact(@Valid @RequestBody ContactRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Contact contact = contactService.addContact(userId, request.getLabel(), request.getAddress());
        return ResponseEntity.ok(contact);
    }

    @GetMapping
    public ResponseEntity<List<Contact>> getContacts() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(contactService.getContacts(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteContact(@PathVariable String id) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        contactService.deleteContact(id, userId);
        return ResponseEntity.ok(Map.of("message", "Contact supprimé"));
    }
}