package com.servicioagua.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    long countByRolInAndEnabled(Collection<Rol> roles, boolean enabled);

    long countByRol(Rol rol);
}