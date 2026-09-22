package com.servicioagua.reporte.dto;

import com.servicioagua.common.enums.EstadoFuga;
import com.servicioagua.common.enums.SeveridadFuga;

import java.time.LocalDate;

public record FugaActivaDto(Long id, String sector, String ubicacion, SeveridadFuga severidad,
                            EstadoFuga estado, LocalDate fechaReporte) {
}