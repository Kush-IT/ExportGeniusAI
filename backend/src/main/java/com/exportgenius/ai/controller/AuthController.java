package com.exportgenius.ai.controller;

import com.exportgenius.ai.dto.AuthResponse;
import com.exportgenius.ai.dto.LoginRequest;
import com.exportgenius.ai.dto.RefreshRequest;
import com.exportgenius.ai.dto.RegisterRequest;
import com.exportgenius.ai.entity.RefreshToken;
import com.exportgenius.ai.entity.Role;
import com.exportgenius.ai.entity.User;
import com.exportgenius.ai.repository.RefreshTokenRepository;
import com.exportgenius.ai.repository.RoleRepository;
import com.exportgenius.ai.repository.UserRepository;
import com.exportgenius.ai.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserRepository userRepository,
                          RoleRepository roleRepository,
                          RefreshTokenRepository refreshTokenRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil,
                          AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is already in use"));
        }

        // Enforce role constraints (EXPORTER or IMPORTER only - Admin accounts are not self-registerable)
        String requestedRoleName = request.getRole().toUpperCase();
        if (!requestedRoleName.equals(Role.EXPORTER) && !requestedRoleName.equals(Role.IMPORTER)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only EXPORTER or IMPORTER registration is allowed"));
        }

        Role role = roleRepository.findByName(requestedRoleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + requestedRoleName));

        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .isActive(false)
                .verificationToken(verificationToken)
                .roles(Set.of(role))
                .build();

        userRepository.save(user);

        // Log the email verification link (simulating sending it for now)
        String verificationLink = "http://localhost:8080/api/auth/verify-email?token=" + verificationToken;
        System.out.println("=================================================");
        System.out.println("SIMULATED EMAIL DISPATCH TO: " + user.getEmail());
        System.out.println("Verification Token: " + verificationToken);
        System.out.println("Please click the link below to verify your account:");
        System.out.println(verificationLink);
        System.out.println("=================================================");

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Registration successful. A verification email has been simulated in logs."
        ));
    }

    @PostMapping("/login")
    @Transactional
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        // Authenticate with AuthenticationManager
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.isActive()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Please verify your email first"));
        }

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), roles);
        String refreshTokenString = jwtUtil.generateRefreshToken(user.getEmail());

        // Save refresh token to database
        LocalDateTime expiry = LocalDateTime.now().plusDays(7);
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenString)
                .expiryDate(expiry)
                .build();

        // Clear prior refresh tokens to prevent bloating
        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.save(refreshToken);

        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .email(user.getEmail())
                .roles(roles)
                .build());
    }

    @PostMapping("/refresh")
    @Transactional
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshToken tokenObj = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (tokenObj.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(tokenObj);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Refresh token has expired"));
        }

        User user = tokenObj.getUser();
        List<String> roles = user.getRoles().stream()
                .map(role -> "ROLE_" + role.getName())
                .collect(Collectors.toList());

        String newAccessToken = jwtUtil.generateAccessToken(user.getEmail(), roles);

        return ResponseEntity.ok(AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(tokenObj.getToken())
                .email(user.getEmail())
                .roles(roles)
                .build());
    }

    @GetMapping("/verify-email")
    @Transactional
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam("token") String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        user.setActive(true);
        user.setVerificationToken(null);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Email verified successfully. You can now log in."));
    }
}
