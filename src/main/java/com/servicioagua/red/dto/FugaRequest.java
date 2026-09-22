package com.servicioagua.red.dto;

import com.servicioagua.common.enums.SeveridadFuga;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FugaRequest {

    @NotNull(message = "El sector es obligatorio")
    private Long sectorId;

    @NotBlank(message = "La ubicación es obligatoria")
    @Size(max = 200, message = "La ubicación no puede superar 200 caracteres")
    private String ubicacion;

    @NotNull(message = "La severidad es obligatoria")
    private SeveridadFuga severidad;
}
