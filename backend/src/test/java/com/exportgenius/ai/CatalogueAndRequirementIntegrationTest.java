package com.exportgenius.ai;

import com.exportgenius.ai.entity.Role;
import com.exportgenius.ai.entity.User;
import com.exportgenius.ai.repository.RoleRepository;
import com.exportgenius.ai.repository.UserRepository;
import com.exportgenius.ai.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CatalogueAndRequirementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String exporterToken;
    private String importerToken;

    @BeforeEach
    public void setUp() {
        // Initialize Roles in database if missing
        Role exporterRole = roleRepository.findByName(Role.EXPORTER)
                .orElseGet(() -> roleRepository.save(Role.builder().id(1).name(Role.EXPORTER).build()));
        Role importerRole = roleRepository.findByName(Role.IMPORTER)
                .orElseGet(() -> roleRepository.save(Role.builder().id(3).name(Role.IMPORTER).build()));

        // Create exporter
        User exporter = userRepository.findByEmail("exporter@test.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("exporter@test.com")
                        .password(passwordEncoder.encode("password123"))
                        .fullName("Test Exporter")
                        .isActive(true)
                        .roles(Set.of(exporterRole))
                        .build()));

        // Create importer
        User importer = userRepository.findByEmail("importer@test.com")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("importer@test.com")
                        .password(passwordEncoder.encode("password123"))
                        .fullName("Test Importer")
                        .isActive(true)
                        .roles(Set.of(importerRole))
                        .build()));

        exporterToken = "Bearer " + jwtUtil.generateAccessToken(exporter.getEmail(), Collections.singletonList("ROLE_EXPORTER"));
        importerToken = "Bearer " + jwtUtil.generateAccessToken(importer.getEmail(), Collections.singletonList("ROLE_IMPORTER"));
    }

    @Test
    public void testImporterCannotSubmitRequirementWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/importer/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productType\":\"Cotton\",\"quantity\":100,\"destinationCountry\":\"Germany\"}"))
                .andExpect(status().isForbidden()); // Security default for unauthenticated is 403/401 depending on config
    }

    @Test
    public void testExporterCannotAccessImporterRequirements() throws Exception {
        mockMvc.perform(get("/api/importer/requirements")
                        .header("Authorization", exporterToken))
                .andExpect(status().isForbidden()); // Returns 403, not 200 with empty list
    }

    @Test
    public void testImporterCanAccessImporterRequirements() throws Exception {
        mockMvc.perform(get("/api/importer/requirements")
                        .header("Authorization", importerToken))
                .andExpect(status().isOk()); // Returns 200
    }
}
