package com.codereview.service;

import com.codereview.dto.AuthDTO;
import com.codereview.entity.RefreshToken;
import com.codereview.entity.User;
import com.codereview.exception.BadRequestException;
import com.codereview.exception.ResourceNotFoundException;
import com.codereview.repository.RefreshTokenRepository;
import com.codereview.repository.UserRepository;
import com.codereview.security.CustomUserDetails;
import com.codereview.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AuditService auditService;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Transactional
    public AuthDTO.AuthResponse register(AuthDTO.RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(User.Role.USER)
                .build();

        user = userRepository.save(Objects.requireNonNull(user));
        auditService.log(user.getId(), user.getUsername(), "REGISTER", "User registered", null);

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthDTO.AuthResponse login(AuthDTO.LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword()));

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userRepository.findById(Objects.requireNonNull(userDetails.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userDetails.getId()));

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        auditService.log(user.getId(), user.getUsername(), "LOGIN", "User logged in", null);

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthDTO.AuthResponse refreshToken(AuthDTO.RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenAndRevokedFalse(request.getRefreshToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired refresh token"));

        if (refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            throw new BadRequestException("Refresh token expired");
        }

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        User user = refreshToken.getUser();
        return generateAuthResponse(user);
    }

    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        User user = userRepository.findById(Objects.requireNonNull(userId)).orElse(null);
        if (user != null) {
            auditService.log(userId, user.getUsername(), "LOGOUT", "User logged out", null);
        }
    }

    public AuthDTO.TokenValidationResponse validateToken(String token) {
        boolean valid = tokenProvider.validateToken(token);
        if (!valid) {
            return new AuthDTO.TokenValidationResponse(false, null);
        }
        String username = tokenProvider.getUsernameFromToken(token);
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return new AuthDTO.TokenValidationResponse(false, null);
        }
        return new AuthDTO.TokenValidationResponse(true, mapToUserInfo(user));
    }

    private AuthDTO.AuthResponse generateAuthResponse(User user) {
        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name());

        String refreshTokenStr = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenStr)
                .user(user)
                .expiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .build();
        refreshTokenRepository.save(Objects.requireNonNull(refreshToken));

        return AuthDTO.AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessTokenExpiration())
                .user(mapToUserInfo(user))
                .build();
    }

    private AuthDTO.UserInfo mapToUserInfo(User user) {
        return AuthDTO.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
