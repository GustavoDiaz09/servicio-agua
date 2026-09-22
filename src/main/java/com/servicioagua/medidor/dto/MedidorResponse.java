package com.servicioagua.medidor.dto;

import com.servicioagua.common.enums.EstadoMedidor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedidorResponse {

    private Long id;
    private String numeroSerie;
    private Long tomaId;
    private String tomaCodigo;
    private Integer lecturaAnteriorAjuste;
    private LocalDate fechaInstalacion;
    private EstadoMedidor estado;
}