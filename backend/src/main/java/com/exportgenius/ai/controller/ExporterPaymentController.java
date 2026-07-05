package com.exportgenius.ai.controller;

import com.exportgenius.ai.entity.PaymentOut;
import com.exportgenius.ai.entity.User;
import com.exportgenius.ai.repository.PaymentOutRepository;
import com.exportgenius.ai.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/exporter/payments")
@PreAuthorize("hasRole('EXPORTER')")
public class ExporterPaymentController {

    private final PaymentOutRepository paymentOutRepository;
    private final UserRepository userRepository;

    public ExporterPaymentController(PaymentOutRepository paymentOutRepository,
                                     UserRepository userRepository) {
        this.paymentOutRepository = paymentOutRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<PaymentOut>> listPayouts(Principal principal) {
        User exporter = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Exporter profile not found"));

        List<PaymentOut> payouts = paymentOutRepository.findByDealCatalogueExporterId(exporter.getId());
        return ResponseEntity.ok(payouts);
    }
}
