package com.exportgenius.ai;

import com.exportgenius.ai.entity.*;
import com.exportgenius.ai.repository.*;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class RazorpayWebhookIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExporterCatalogueRepository catalogueRepository;

    @Autowired
    private RequirementRepository requirementRepository;

    @Autowired
    private DealRepository dealRepository;

    @Autowired
    private PaymentInRepository paymentInRepository;

    private Deal deal;
    private PaymentIn paymentIn;
    private final String orderId = "order_abc123xyz";
    private final String paymentId = "pay_999xxx";

    @BeforeEach
    public void setUp() {
        Role exporterRole = roleRepository.findByName(Role.EXPORTER)
                .orElseGet(() -> roleRepository.save(Role.builder().id(1).name(Role.EXPORTER).build()));
        Role importerRole = roleRepository.findByName(Role.IMPORTER)
                .orElseGet(() -> roleRepository.save(Role.builder().id(3).name(Role.IMPORTER).build()));

        User exporter = userRepository.save(User.builder()
                .email("exporter_webhook@test.com")
                .password("password")
                .fullName("Webhook Exporter")
                .isActive(true)
                .roles(Set.of(exporterRole))
                .build());

        User importer = userRepository.save(User.builder()
                .email("importer_webhook@test.com")
                .password("password")
                .fullName("Webhook Importer")
                .isActive(true)
                .roles(Set.of(importerRole))
                .build());

        Category category = categoryRepository.save(Category.builder().id(12).name("Textiles Webhook").build());

        ExporterCatalogue catalogueItem = catalogueRepository.save(ExporterCatalogue.builder()
                .exporter(exporter)
                .title("Silk Yarn")
                .category(category)
                .hsCode("500200")
                .supplyPrice(BigDecimal.valueOf(15.00))
                .moq(50)
                .leadTimeDays(10)
                .isActive(true)
                .build());

        Requirement requirement = requirementRepository.save(Requirement.builder()
                .importer(importer)
                .productType("Silk Yarn")
                .quantity(100)
                .destinationCountry("France")
                .status("OPEN")
                .build());

        deal = dealRepository.save(Deal.builder()
                .requirement(requirement)
                .catalogue(catalogueItem)
                .quantity(100)
                .supplyPrice(BigDecimal.valueOf(15.00))
                .sellPrice(BigDecimal.valueOf(20.00))
                .marginAmount(BigDecimal.valueOf(500.00))
                .marginPct(BigDecimal.valueOf(0.25))
                .stage(DealStage.CONFIRMED)
                .importerAccepted(true)
                .build());

        paymentIn = paymentInRepository.save(PaymentIn.builder()
                .deal(deal)
                .amount(BigDecimal.valueOf(2000.00))
                .status("PENDING")
                .razorpayRef(orderId)
                .build());
    }

    @Test
    public void testWebhookSignatureVerificationFails() throws Exception {
        String testPayload = "{\"event\":\"payment.captured\"}";
        
        mockMvc.perform(post("/api/webhooks/razorpay")
                        .header("X-Razorpay-Signature", "invalid_sig")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testPayload))
                .andExpect(status().isBadRequest());
        
        // Assert state is still PENDING
        PaymentIn refreshedPayment = paymentInRepository.findById(paymentIn.getId()).orElseThrow();
        assertEquals("PENDING", refreshedPayment.getStatus());
    }

    @Test
    public void testWebhookSuccessUpdatesPaymentInToReceived() throws Exception {
        // Construct mock Razorpay payload JSON
        JSONObject payloadObj = new JSONObject();
        payloadObj.put("event", "payment.captured");
        
        JSONObject paymentEntity = new JSONObject();
        paymentEntity.put("order_id", orderId);
        paymentEntity.put("id", paymentId);
        
        JSONObject entityWrapper = new JSONObject();
        entityWrapper.put("entity", paymentEntity);
        
        JSONObject payloadWrapper = new JSONObject();
        payloadWrapper.put("payment", entityWrapper);
        
        payloadObj.put("payload", payloadWrapper);
        
        String requestBody = payloadObj.toString();
        String testSignature = "valid_mock_signature";

        // Mock Razorpay SDK signature verification using Mockito static mock
        try (MockedStatic<Utils> utilities = mockStatic(Utils.class)) {
            utilities.when(() -> Utils.verifyWebhookSignature(anyString(), anyString(), anyString()))
                    .thenReturn(true);

            mockMvc.perform(post("/api/webhooks/razorpay")
                            .header("X-Razorpay-Signature", testSignature)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            // Assert state has been updated to RECEIVED
            PaymentIn refreshedPayment = paymentInRepository.findById(paymentIn.getId()).orElseThrow();
            assertEquals("RECEIVED", refreshedPayment.getStatus());
            assertEquals(paymentId, refreshedPayment.getRazorpayRef());
        }
    }
}
