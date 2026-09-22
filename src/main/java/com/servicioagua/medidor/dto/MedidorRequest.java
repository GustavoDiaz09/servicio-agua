package com.servicioagua.medidor.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedidorRequest {

    @NotBlank(message = "El número de serie es obligatorio")
    @Size(max = 50, message = "El número de serie no puede superar 50 caracteres")
    private String numeroSerie;

    @Min(value = 0, message = "La lectura anterior de ajuste no puede ser negativa")
    private Integer lecturaAnteriorAjuste;
}