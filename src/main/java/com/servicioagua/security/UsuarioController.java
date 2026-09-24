package com.servicioagua.security;

import com.servicioagua.common.dto.ApiResponse;
import com.servicioagua.security.dto.CrearUsuarioRequest;
import com.servicioagua.security.dto.EstadoUsuarioRequest;
import com.servicioagua.security.dto.ResetPasswordRequest;
import com.servicioagua.security.dto.UsuarioDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UsuarioDto>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok(usuarioService.listar()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UsuarioDto>> crear(@Valid @RequestBody CrearUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Usuario creado exitosamente", usuarioService.crear(request)));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<UsuarioDto>> cambiarEstado(
            @PathVariable Long id, @Valid @RequestBody EstadoUsuarioRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Estado del usuario actualizado correctamente",
                usuarioService.cambiarEstado(id, request)));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        usuarioService.resetPassword(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Contraseña actualizada correctamente", null));
    }
}