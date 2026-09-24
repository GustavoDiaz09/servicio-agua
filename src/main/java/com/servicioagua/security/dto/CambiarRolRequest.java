package com.servicioagua.security.dto;

import com.servicioagua.security.Rol;
import jakarta.validation.constraints.NotNull;

public record CambiarRolRequest(@NotNull(message = "El rol es obligatorio") Rol rol) {
}