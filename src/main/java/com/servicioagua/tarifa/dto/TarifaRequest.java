package com.servicioagua.tarifa.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class TarifaRequest {

    @NotBlank(message = "El nombre de la tarifa es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @NotNull(message = "El tramo inicial (desdeM3) es obligatorio")
    @Min(value = 0, message = "El tramo inicial (desdeM3) no puede ser negativo")
    private Integer desdeM3;

    @NotNull(message = "El tramo final (hastaM3) es obligatorio")
    @Min(value = 1, message = "El tramo final (hastaM3) debe ser al menos 1")
    private Integer hastaM3;

    @NotNull(message = "El precio por m3 es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio por m3 no puede ser negativo")
    private BigDecimal precioPorM3;

    @DecimalMin(value = "0.00", message = "El cargo fijo no puede ser negativo")
    private BigDecimal cargoFijo;

    @NotNull(message = "La fecha de inicio de vigencia es obligatoria")
    private LocalDate vigenteDesde;

    private LocalDate vigenteHasta;
}