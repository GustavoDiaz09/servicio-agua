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

    @NotNull(message = "El cliente es obligatorio")
    private Long clienteId;

    @NotNull(message = "El período desde es obligatorio")
    private LocalDate periodoDesde;

    @NotNull(message = "El período hasta es obligatorio")
    private LocalDate periodoHasta;

    @NotNull(message = "El vencimiento es obligatorio")
    private LocalDate vencimiento;
}
