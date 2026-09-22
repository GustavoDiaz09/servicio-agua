package com.servicioagua.reporte.dto;

import java.math.BigDecimal;
import java.util.List;

public record ClienteMorosoDto(Long clienteId, String documento, String nombre, BigDecimal deudaTotal,
                               Integer cantidadFacturas, List<FacturaMorosaDto> facturas) {
}