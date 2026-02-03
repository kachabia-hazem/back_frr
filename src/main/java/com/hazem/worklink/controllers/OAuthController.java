package com.hazem.worklink.controllers;

import com.hazem.worklink.dto.request.OAuthGoogleRequest;
import com.hazem.worklink.dto.request.OAuthLinkedInCompleteRequest;
import com.hazem.worklink.dto.request.OAuthLinkedInRequest;
import com.hazem.worklink.dto.response.AuthResponse;
import com.hazem.worklink.services.OAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService oAuthService;

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody OAuthGoogleRequest request) {
        try {
            AuthResponse response = oAuthService.googleLogin(request.getIdToken());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, null, null, null, e.getMessage()));
        }
    }

    @PostMapping("/linkedin")
    public ResponseEntity<AuthResponse> linkedInLogin(@Valid @RequestBody OAuthLinkedInRequest request) {
        try {
            AuthResponse response = oAuthService.linkedInLogin(request.getCode());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, null, null, null, e.getMessage()));
        }
    }

    @PostMapping("/linkedin/complete-registration")
    public ResponseEntity<AuthResponse> linkedInCompleteRegistration(
            @Valid @RequestBody OAuthLinkedInCompleteRequest request) {
        try {
            AuthResponse response = oAuthService.linkedInCompleteRegistration(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse(null, null, null, null, e.getMessage()));
        }
    }
}
