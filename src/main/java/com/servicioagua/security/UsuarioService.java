package com.servicioagua.security;

import com.servicioagua.common.exception.BusinessException;
import com.servicioagua.common.exception.DuplicateResourceException;
import com.servicioagua.common.exception.ResourceNotFoundException;
import com.servicioagua.security.dto.CambiarRolRequest;
import com.servicioagua.security.dto.CrearUsuarioRequest;
import com.servicioagua.security.dto.EstadoUsuarioRequest;
import com.servicioagua.security.dto.ResetPasswordRequest;
import com.servicioagua.security.dto.UsuarioDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UsuarioDto> listar() {
        return usuarioRepository.findAll().stream().map(UsuarioDto::from).toList();
    }

    @Transactional
    public UsuarioDto crear(CrearUsuarioRequest request) {
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("El usuario '" + request.getUsername() + "' ya está registrado");
        }
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .rol(Rol.ADMIN)
                .enabled(true)
                .build());
        return UsuarioDto.from(usuario);
    }

    @Transactional
    public UsuarioDto cambiarEstado(Long id, EstadoUsuarioRequest request) {
        Usuario usuario = obtener(id);
        if (!Boolean.TRUE.equals(request.getEnabled()) && usuario.isEnabled()) {
            String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
            if (usuario.getUsername().equals(usuarioActual)) {
                throw new BusinessException("No puede deshabilitarse a sí mismo");
            }
            if (usuarioRepository.countByRolInAndEnabled(List.of(Rol.ADMIN, Rol.SUPERADMIN), true) <= 1) {
                throw new BusinessException("No puede deshabilitar al último administrador habilitado");
            }
        }
        usuario.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        return UsuarioDto.from(usuarioRepository.save(usuario));
    }

    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest request) {
        Usuario usuario = obtener(id);
        usuario.setPassword(passwordEncoder.encode(request.getNewPassword()));
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void eliminar(Long id) {
        Usuario usuario = obtener(id);
        validarGestionSegura(usuario);
        if (usuario.getRol() == Rol.SUPERADMIN && usuario.isEnabled()
                && usuarioRepository.countByRolInAndEnabled(List.of(Rol.SUPERADMIN), true) <= 1) {
            throw new BusinessException("No puede eliminar al único superadministrador habilitado");
        }
        if (usuarioRepository.countByRolInAndEnabled(List.of(Rol.ADMIN, Rol.SUPERADMIN), true) <= 1) {
            throw new BusinessException("No puede eliminar al último administrador habilitado");
        }
        usuarioRepository.delete(usuario);
    }

    @Transactional
    public UsuarioDto cambiarRol(Long id, CambiarRolRequest request) {
        Usuario usuario = obtener(id);
        if (request.rol() == usuario.getRol()) {
            return UsuarioDto.from(usuario);
        }
        validarGestionSegura(usuario);
        if (request.rol() == Rol.SUPERADMIN && usuario.getRol() != Rol.SUPERADMIN
                && usuarioRepository.countByRol(Rol.SUPERADMIN) >= 1) {
            throw new BusinessException("Ya existe un superadministrador; solo puede existir uno");
        }
        if (usuario.getRol() == Rol.SUPERADMIN && usuario.isEnabled()
                && usuarioRepository.countByRolInAndEnabled(List.of(Rol.SUPERADMIN), true) <= 1) {
            throw new BusinessException("No puede quitar el rol de superadministrador al único superadministrador habilitado");
        }
        usuario.setRol(request.rol());
        return UsuarioDto.from(usuarioRepository.save(usuario));
    }

    private void validarGestionSegura(Usuario usuario) {
        String usuarioActual = SecurityContextHolder.getContext().getAuthentication().getName();
        if (usuario.getUsername().equals(usuarioActual)) {
            throw new BusinessException("No puede gestionar su propio usuario");
        }
    }

    private Usuario obtener(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }
}