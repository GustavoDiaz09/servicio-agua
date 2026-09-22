package com.servicioagua.factura.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerarFacturaRequest {

    @NotNull(message = "el cliente es obligatorio")
    private Long clienteId;

    @NotNull(message = "el período desde es obligatorio")
    private LocalDate periodoDesde;

    @NotNull(message = "el período hasta es obligatorio")
    private LocalDate periodoHasta;

    @NotNull(message = "el vencimiento es obligatorio")
    private LocalDate vencimiento;
}
