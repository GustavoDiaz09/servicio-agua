package com.servicioagua.reporte.dto;

import java.math.BigDecimal;

public record VentaPeriodoDto(String periodo, BigDecimal totalVendido) {
}