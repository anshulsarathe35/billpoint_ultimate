package com.billpoint.backend.controller;

import com.billpoint.backend.model.Bill;
import com.billpoint.backend.model.User;
import com.billpoint.backend.model.Offer;
import com.billpoint.backend.repository.BillRepository;
import com.billpoint.backend.repository.UserRepository;
import com.billpoint.backend.repository.OfferRepository;
import com.billpoint.backend.security.UserDetailsImpl;
import com.billpoint.backend.service.InvoiceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/customer")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerPortalController {

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InvoiceService invoiceService;

    @GetMapping("/bills")
    public ResponseEntity<?> getMyBills(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Error: User not found"));

        if (user.getPhone() == null) {
            return ResponseEntity.ok(List.of());
        }

        List<Bill> bills = billRepository.findByCustomer_Phone(user.getPhone());
        return ResponseEntity.ok(bills);
    }

    @GetMapping("/offers")
    public ResponseEntity<?> getOffers() {
        List<Offer> activeOffers = offerRepository.findAll().stream()
                .filter(offer -> offer.getIsActive() != null && offer.getIsActive())
                .toList();
        return ResponseEntity.ok(activeOffers);
    }

    @GetMapping("/bills/{id}/invoice")
    public ResponseEntity<byte[]> getInvoice(@PathVariable Long id, Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Error: User not found"));

        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        // Security check: ensure the bill belongs to this customer's phone number
        if (bill.getCustomer() == null || !bill.getCustomer().getPhone().equals(user.getPhone())) {
             return ResponseEntity.status(403).build();
        }

        byte[] pdf = invoiceService.generateInvoicePdf(bill);

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=invoice_" + id + ".pdf")
                .body(pdf);
    }
}