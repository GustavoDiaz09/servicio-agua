package com.servicioagua.red.dto;

import jakarta.validation.constraints.DecimalMin;
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
public class TuberiaRequest {

    @NotNull(message = "El sector es obligatorio")
    private Long sectorId;

    @NotBlank(message = "El material es obligatorio")
    @Size(max = 60, message = "El material no puede superar 60 caracteres")
    private String material;

    @NotNull(message = "El diámetro es obligatorio")
    @DecimalMin(value = "0", inclusive = false, message = "El diámetro debe ser positivo")
    private BigDecimal diametroMm;

    @NotNull(message = "La longitud es obligatoria")
    @DecimalMin(value = "0", inclusive = false, message = "La longitud debe ser positiva")
    private BigDecimal longitudM;

    @Size(max = 100, message = "El tramo no puede superar 100 caracteres")
    private String tramo;

    private LocalDate fechaInstalacion;
}
