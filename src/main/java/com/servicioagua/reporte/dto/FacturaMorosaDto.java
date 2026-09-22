package com.servicioagua.reporte.dto;

import com.servicioagua.common.enums.EstadoFactura;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FacturaMorosaDto(Long id, String numero, BigDecimal total, LocalDate vencimiento,
                               LocalDate periodoDesde, LocalDate periodoHasta, EstadoFactura estado) {
}