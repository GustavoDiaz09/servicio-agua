package com.servicioagua.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final SecurityProperties properties;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
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