package com.servicioagua.red.dto;

import com.servicioagua.common.enums.EstadoMantenimiento;
import com.servicioagua.common.enums.TipoMantenimiento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MantenimientoResponse {

    private Long id;
    private Long sectorId;
    private String sectorNombre;
    private String descripcion;
    private TipoMantenimiento tipo;
    private LocalDate fechaProgramada;
    private LocalDate fechaFin;
    private EstadoMantenimiento estado;
}
