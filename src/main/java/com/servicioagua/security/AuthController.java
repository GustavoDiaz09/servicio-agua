package com.servicioagua.security;

import com.servicioagua.common.dto.ApiResponse;
import com.servicioagua.security.dto.CambioPasswordRequest;
import com.servicioagua.security.dto.LoginRequest;
import com.servicioagua.security.dto.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Login correcto", authService.login(request)));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> cambiarMiPassword(@Valid @RequestBody CambioPasswordRequest request) {
        authService.cambiarMiPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Contraseña actualizada correctamente", null));
    }
}