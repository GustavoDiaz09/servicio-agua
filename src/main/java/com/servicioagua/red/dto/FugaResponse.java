package com.servicioagua.red.dto;

import com.servicioagua.common.enums.EstadoFuga;
import com.servicioagua.common.enums.SeveridadFuga;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FugaResponse {

    private Long id;
    private Long sectorId;
    private String sectorNombre;
    private String ubicacion;
    private SeveridadFuga severidad;
    private EstadoFuga estado;
    private LocalDate fechaReporte;
    private LocalDate fechaResolucion;
}
