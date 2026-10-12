package com.servicioagua.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final SecurityProperties properties;

    private SecretKey key;

    @PostConstruct
    void init() {
        String secret = properties.getJwtSecret();
        if (secret == null || secret.isBlank()) {
            key = Jwts.SIG.HS256.key().build();
            log.warn("JWT_SECRET no está configurado. Se generó una clave aleatoria de sesión: los tokens "
                    + "dejarán de ser válidos al reiniciar la aplicación. Configure JWT_SECRET en producción.");
        } else {
            key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }

    private SecretKey key() {
        return key;
    }

    public String generarToken(Usuario usuario) {
        Date now = new Date();
        return Jwts.builder()
                .subject(usuario.getUsername())
                .claim("rol", usuario.getRol().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + properties.getJwtExpirationMs()))
                .signWith(key())
                .compact();
    }

    public String extraerUsuario(String token) {
        return claims(token).getSubject();
    }

    public boolean esValido(String token, UserDetails userDetails) {
        Claims claims = claims(token);
        return claims.getSubject().equals(userDetails.getUsername())
                && claims.getExpiration().after(new Date());
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}