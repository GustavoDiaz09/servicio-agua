package com.servicioagua.red.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TuberiaResponse {

    private Long id;
    private Long sectorId;
    private String sectorNombre;
    private String material;
    private BigDecimal diametroMm;
    private BigDecimal longitudM;
    private String tramo;
    private LocalDate fechaInstalacion;
}
