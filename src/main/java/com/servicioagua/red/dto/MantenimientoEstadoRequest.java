package com.servicioagua.red.dto;

import com.servicioagua.common.enums.EstadoMantenimiento;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MantenimientoEstadoRequest {

    @NotNull(message = "El estado es obligatorio")
    private EstadoMantenimiento estado;
}
