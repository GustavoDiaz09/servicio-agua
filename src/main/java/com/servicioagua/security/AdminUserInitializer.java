package com.servicioagua.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements ApplicationRunner {

    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int LONGITUD_PASSWORD = 16;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isAdminAutoCreate()) {
            return;
        }
        if (usuarioRepository.count() > 0) {
            return;
        }
        crearUsuario(properties.getAdminUsername(), properties.getAdminPassword(), Rol.ADMIN);
        crearUsuario(properties.getSuperadminUsername(), properties.getSuperadminPassword(), Rol.SUPERADMIN);
    }

    private void crearUsuario(String username, String password, Rol rol) {
        boolean generada = password == null || password.isBlank();
        String passwordFinal = generada ? generarPassword() : password;
        usuarioRepository.save(Usuario.builder()
                .username(username)
                .password(passwordEncoder.encode(passwordFinal))
                .rol(rol)
                .enabled(true)
                .build());
        if (generada) {
            log.warn("Contraseña del usuario '{}' no configurada. Se generó una contraseña temporal: '{}'. "
                    + "Cámbiela tras el primer inicio de sesión y defina la variable de entorno correspondiente.",
                    username, passwordFinal);
        }
    }

    private String generarPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(LONGITUD_PASSWORD);
        for (int i = 0; i < LONGITUD_PASSWORD; i++) {
            sb.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }
}