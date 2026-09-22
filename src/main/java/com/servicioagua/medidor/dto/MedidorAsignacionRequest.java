package com.servicioagua.medidor.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedidorAsignacionRequest {

    @NotNull(message = "La toma es obligatoria")
    private Long tomaId;
}