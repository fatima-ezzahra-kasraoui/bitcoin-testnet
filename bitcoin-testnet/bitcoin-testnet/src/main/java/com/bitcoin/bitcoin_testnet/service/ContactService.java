package com.bitcoin.bitcoin_testnet.service;

import com.bitcoin.bitcoin_testnet.model.Contact;
import com.bitcoin.bitcoin_testnet.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactRepository contactRepository;

    public Contact addContact(String userId, String label, String address) {
        Contact contact = new Contact();
        contact.setUserId(userId);
        contact.setLabel(label);
        contact.setAddress(address);
        contact.setCreatedAt(new Date());
        return contactRepository.save(contact);
    }

    public List<Contact> getContacts(String userId) {
        return contactRepository.findByUserId(userId);
    }

    public void deleteContact(String id, String userId) {
        contactRepository.deleteByIdAndUserId(id, userId);
    }
}