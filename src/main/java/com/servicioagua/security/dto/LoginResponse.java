package com.servicioagua.security.dto;

import com.servicioagua.security.Rol;

public record LoginResponse(String token, String username, Rol rol) {
}