package com.servicioagua.lectura.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturaRequest {

    @NotNull(message = "El medidor es obligatorio")
    private Long medidorId;

    @NotNull(message = "El valor de lectura es obligatorio")
    @Min(value = 0, message = "El valor de lectura no puede ser negativo")
    private Integer valorLectura;

    @NotNull(message = "La fecha de lectura es obligatoria")
    private LocalDate fechaLectura;

    @Size(max = 255, message = "La observación no puede superar los 255 caracteres")
    private String observacion;
}