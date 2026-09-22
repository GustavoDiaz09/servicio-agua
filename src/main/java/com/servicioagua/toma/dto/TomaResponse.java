package com.servicioagua.toma.dto;

import com.servicioagua.common.enums.EstadoToma;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TomaResponse {

    private Long id;
    private String codigo;
    private Long sectorId;
    private String sectorNombre;
    private Long clienteId;
    private String clienteNombre;
    private LocalDate fechaAlta;
    private EstadoToma estado;
}