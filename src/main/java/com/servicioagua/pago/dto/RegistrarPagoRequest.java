package com.servicioagua.pago.dto;

import com.servicioagua.common.enums.MetodoPago;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarPagoRequest {

    @NotNull(message = "la factura es obligatoria")
    private Long facturaId;

    @NotNull(message = "el monto es obligatorio")
    private BigDecimal monto;

    @NotNull(message = "el método de pago es obligatorio")
    private MetodoPago metodo;

    private String referencia;
}