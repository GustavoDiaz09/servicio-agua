package com.servicioagua.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private String jwtSecret;

    private long jwtExpirationMs = 86_400_000;

    private boolean adminAutoCreate = true;

    private String adminUsername = "admin";

    private String adminPassword;

    private String superadminUsername = "gustavo";

    private String superadminPassword;
}