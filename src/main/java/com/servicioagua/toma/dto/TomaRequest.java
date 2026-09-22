package com.servicioagua.toma.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TomaRequest {

    @NotBlank(message = "El código es obligatorio")
    @Size(max = 40, message = "El código no puede superar 40 caracteres")
    private String codigo;

    @NotNull(message = "El sector es obligatorio")
    private Long sectorId;

    @NotNull(message = "El cliente es obligatorio")
    private Long clienteId;
}