package com.codereview.controller;

import com.codereview.dto.ApiResponse;
import com.codereview.dto.AuthDTO;
import com.codereview.security.CustomUserDetails;
import com.codereview.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthDTO.AuthResponse>> register(@Valid @RequestBody AuthDTO.RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Registration successful", authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDTO.AuthResponse>> login(@Valid @RequestBody AuthDTO.LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthDTO.AuthResponse>> refresh(@Valid @RequestBody AuthDTO.RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.refreshToken(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal CustomUserDetails user) {
        authService.logout(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<AuthDTO.TokenValidationResponse>> validate(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return ResponseEntity.ok(ApiResponse.success(authService.validateToken(token)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthDTO.UserInfo>> getCurrentUser(@AuthenticationPrincipal CustomUserDetails user) {
        AuthDTO.UserInfo info = AuthDTO.UserInfo.builder()
                .id(user.getId()).username(user.getUsername())
                .email(user.getEmail()).role(user.getRole()).build();
        return ResponseEntity.ok(ApiResponse.success(info));
    }
}
