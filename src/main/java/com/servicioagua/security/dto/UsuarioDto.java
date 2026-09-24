package com.servicioagua.security.dto;

import com.servicioagua.security.Rol;
import com.servicioagua.security.Usuario;

import java.time.LocalDateTime;

public record UsuarioDto(Long id, String username, Rol rol, boolean enabled, LocalDateTime fechaCreacion) {

    public static UsuarioDto from(Usuario usuario) {
        return new UsuarioDto(usuario.getId(), usuario.getUsername(), usuario.getRol(),
                usuario.isEnabled(), usuario.getFechaCreacion());
    }
}