package com.servicioagua.red.dto;

import com.servicioagua.common.enums.TipoMantenimiento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MantenimientoRequest {

    @NotNull(message = "El sector es obligatorio")
    private Long sectorId;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
    private String descripcion;

    @NotNull(message = "El tipo es obligatorio")
    private TipoMantenimiento tipo;

    @NotNull(message = "La fecha programada es obligatoria")
    private LocalDate fechaProgramada;
}
