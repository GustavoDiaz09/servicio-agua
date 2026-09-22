package com.servicioagua.pago.dto;

import com.servicioagua.common.enums.EstadoFactura;
import com.servicioagua.common.enums.MetodoPago;
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
public class PagoResponse {

    private Long id;
    private Long facturaId;
    private String facturaNumero;
    private BigDecimal monto;
    private LocalDate fechaPago;
    private MetodoPago metodo;
    private String referencia;
    private EstadoFactura estadoFactura;
}