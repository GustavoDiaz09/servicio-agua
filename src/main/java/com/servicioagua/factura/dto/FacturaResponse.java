package com.servicioagua.factura.dto;

import com.servicioagua.common.enums.EstadoFactura;
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
public class FacturaResponse {

    private Long id;
    private String numero;
    private Long clienteId;
    private String clienteNombre;
    private Integer consumoM3;
    private BigDecimal subtotal;
    private BigDecimal cargoFijo;
    private BigDecimal total;
    private LocalDate periodoDesde;
    private LocalDate periodoHasta;
    private LocalDate vencimiento;
    private EstadoFactura estado;
}
