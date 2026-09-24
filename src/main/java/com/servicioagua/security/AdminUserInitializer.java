package com.servicioagua.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements ApplicationRunner {

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
        usuarioRepository.save(Usuario.builder()
                .username(properties.getAdminUsername())
                .password(passwordEncoder.encode(properties.getAdminPassword()))
                .rol(Rol.ADMIN)
                .enabled(true)
                .build());
        usuarioRepository.save(Usuario.builder()
                .username(properties.getSuperadminUsername())
                .password(passwordEncoder.encode(properties.getSuperadminPassword()))
                .rol(Rol.SUPERADMIN)
                .enabled(true)
                .build());
    }
}