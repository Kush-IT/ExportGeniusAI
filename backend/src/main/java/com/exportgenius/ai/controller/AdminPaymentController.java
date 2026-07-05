package com.exportgenius.ai.controller;

import com.exportgenius.ai.entity.PaymentIn;
import com.exportgenius.ai.entity.PaymentOut;
import com.exportgenius.ai.repository.PaymentInRepository;
import com.exportgenius.ai.repository.PaymentOutRepository;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

    private final PaymentOutRepository paymentOutRepository;
    private final PaymentInRepository paymentInRepository;

    public AdminPaymentController(PaymentOutRepository paymentOutRepository,
                                  PaymentInRepository paymentInRepository) {
        this.paymentOutRepository = paymentOutRepository;
        this.paymentInRepository = paymentInRepository;
    }

    @GetMapping("/out")
    public ResponseEntity<List<PaymentOut>> listPayouts(@RequestParam(value = "status", required = false) String status) {
        List<PaymentOut> payouts = paymentOutRepository.findAll();
        if (status != null && !status.isEmpty()) {
            payouts = payouts.stream()
                    .filter(p -> p.getStatus().equalsIgnoreCase(status))
                    .collect(Collectors.toList());
        }
        return ResponseEntity.ok(payouts);
    }

    @PatchMapping("/out/{id}/settle")
    @Transactional
    public ResponseEntity<?> settlePayout(@PathVariable("id") UUID id, @RequestBody SettlePayoutRequest request) {
        PaymentOut payout = paymentOutRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payout record not found"));

        if (request.getBankRef() == null || request.getBankRef().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Bank reference number is required to settle."));
        }

        payout.setStatus("SETTLED");
        payout.setBankRef(request.getBankRef());
        PaymentOut saved = paymentOutRepository.save(payout);

        return ResponseEntity.ok(saved);
    }

    @GetMapping("/margin-report")
    public ResponseEntity<Map<String, Object>> getMarginReport() {
        // Net Margin = Sum(payments_in.amount where status == RECEIVED) - Sum(payments_out.amount where status == SETTLED)
        List<PaymentIn> paymentsIn = paymentInRepository.findAll().stream()
                .filter(p -> "RECEIVED".equalsIgnoreCase(p.getStatus()))
                .collect(Collectors.toList());

        List<PaymentOut> paymentsOut = paymentOutRepository.findAll().stream()
                .filter(p -> "SETTLED".equalsIgnoreCase(p.getStatus()))
                .collect(Collectors.toList());

        BigDecimal totalIn = paymentsIn.stream()
                .map(PaymentIn::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOut = paymentsOut.stream()
                .map(PaymentOut::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netMargin = totalIn.subtract(totalOut);

        Map<String, Object> report = new HashMap<>();
        report.put("totalCollectedAmount", totalIn);
        report.put("totalPaidAmount", totalOut);
        report.put("netMarginAmount", netMargin);
        report.put("verifiedInPaymentsCount", paymentsIn.size());
        report.put("settledOutPaymentsCount", paymentsOut.size());

        return ResponseEntity.ok(report);
    }

    @Data
    public static class SettlePayoutRequest {
        private String bankRef;
    }
}
