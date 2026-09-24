package com.servicioagua.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private String jwtSecret = "servicio-agua-d3v-2026-clave-jwt-hs256-minimo-32-bytes-abcdef0123456789";

    private long jwtExpirationMs = 86_400_000;

    private boolean adminAutoCreate = true;

    private String adminUsername = "admin";

    private String adminPassword = "admin123";

    private String superadminUsername = "gustavo";

    private String superadminPassword = "admin123";
}