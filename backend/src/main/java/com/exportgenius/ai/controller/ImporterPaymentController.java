package com.exportgenius.ai.controller;

import com.exportgenius.ai.entity.*;
import com.exportgenius.ai.repository.DealRepository;
import com.exportgenius.ai.repository.PaymentInRepository;
import com.exportgenius.ai.repository.UserRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping
public class ImporterPaymentController {

    private final DealRepository dealRepository;
    private final UserRepository userRepository;
    private final PaymentInRepository paymentInRepository;

    @Value("${razorpay.key-id:rzp_test_592913036}")
    private String keyId;

    @Value("${razorpay.key-secret:rzp_test_secret_3036}")
    private String keySecret;

    @Value("${razorpay.webhook-secret:rzp_webhook_secret_3036}")
    private String webhookSecret;

    public ImporterPaymentController(DealRepository dealRepository,
                                     UserRepository userRepository,
                                     PaymentInRepository paymentInRepository) {
        this.dealRepository = dealRepository;
        this.userRepository = userRepository;
        this.paymentInRepository = paymentInRepository;
    }

    @PostMapping("/api/importer/payments/initiate")
    @PreAuthorize("hasRole('IMPORTER')")
    @Transactional
    public ResponseEntity<?> initiatePayment(@RequestBody Map<String, String> body, Principal principal) {
        String dealIdStr = body.get("dealId");
        if (dealIdStr == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "dealId is required"));
        }

        UUID dealId = UUID.fromString(dealIdStr);
        User importer = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Importer profile not found"));

        Deal deal = dealRepository.findByIdAndRequirementImporter(dealId, importer)
                .orElseThrow(() -> new IllegalArgumentException("Deal not found or does not belong to you"));

        // Deal must be in CONFIRMED stage or later
        if (deal.getStage() == DealStage.SOURCING || deal.getStage() == DealStage.QUOTED || deal.getStage() == DealStage.NEGOTIATING) {
            return ResponseEntity.badRequest().body(Map.of("error", "Payment can only be initiated on a CONFIRMED deal"));
        }

        try {
            // Amount is sellPrice * quantity
            BigDecimal totalSellPrice = deal.getSellPrice().multiply(BigDecimal.valueOf(deal.getQuantity()));
            // Razorpay amount must be in paise (amount * 100)
            BigDecimal amountInPaise = totalSellPrice.multiply(BigDecimal.valueOf(100));

            RazorpayClient razorpayClient = new RazorpayClient(keyId, keySecret);
            
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise.intValue());
            orderRequest.put("currency", "INR"); // Razorpay test accounts use INR by default
            orderRequest.put("receipt", "rcpt_" + deal.getId().toString().substring(0, 8));

            Order rzpOrder = razorpayClient.orders.create(orderRequest);
            String rzpOrderId = rzpOrder.get("id");

            // Look up corresponding PaymentIn record and save the order ID reference
            PaymentIn paymentIn = paymentInRepository.findFirstByDealOrderByCreatedAtDesc(deal)
                    .orElseGet(() -> PaymentIn.builder().deal(deal).amount(totalSellPrice).build());
            
            paymentIn.setRazorpayRef(rzpOrderId);
            paymentInRepository.save(paymentIn);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", rzpOrderId);
            response.put("amount", amountInPaise.intValue());
            response.put("currency", "INR");
            response.put("keyId", keyId);
            response.put("dealId", deal.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to initiate Razorpay order: " + e.getMessage()));
        }
    }

    @PostMapping("/api/webhooks/razorpay")
    @Transactional
    public ResponseEntity<Map<String, String>> processRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {

        try {
            // Verify signature using SDK utilities
            boolean isValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
            if (!isValid) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid signature"));
            }

            JSONObject jsonObject = new JSONObject(payload);
            String event = jsonObject.optString("event");

            if ("payment.captured".equals(event)) {
                JSONObject paymentEntity = jsonObject.getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");

                String orderId = paymentEntity.optString("order_id");
                String paymentId = paymentEntity.optString("id");

                // Find corresponding payment_in row
                paymentInRepository.findAll().stream()
                        .filter(p -> orderId.equals(p.getRazorpayRef()))
                        .findFirst()
                        .ifPresent(paymentIn -> {
                            paymentIn.setStatus("RECEIVED");
                            paymentIn.setRazorpayRef(paymentId);
                            paymentInRepository.save(paymentIn);
                            System.out.println("Razorpay Webhook: Verified payment captured. Status updated to RECEIVED for order " + orderId);
                        });
            }

            return ResponseEntity.ok(Map.of("status", "processed"));

        } catch (Exception e) {
            System.err.println("Webhook processing failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/importer/payments/simulate/{paymentId}")
    @PreAuthorize("hasRole('IMPORTER')")
    @Transactional
    public ResponseEntity<?> simulatePaymentSuccess(@PathVariable("paymentId") UUID paymentId) {
        PaymentIn paymentIn = paymentInRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment record not found"));

        paymentIn.setStatus("RECEIVED");
        paymentIn.setRazorpayRef("sim_" + UUID.randomUUID().toString().substring(0, 8));
        PaymentIn saved = paymentInRepository.save(paymentIn);

        return ResponseEntity.ok(saved);
    }
}
