package com.servicioagua.lectura.dto;

import com.servicioagua.common.enums.EstadoLectura;
import com.servicioagua.lectura.Lectura;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class LecturaResponse {

    private Long id;
    private Long medidorId;
    private String numeroSerie;
    private Integer valorLectura;
    private Integer consumoM3;
    private LocalDate fechaLectura;
    private EstadoLectura estado;
    private String observacion;

    public static LecturaResponse fromEntity(Lectura lectura) {
        return LecturaResponse.builder()
                .id(lectura.getId())
                .medidorId(lectura.getMedidor().getId())
                .numeroSerie(lectura.getMedidor().getNumeroSerie())
                .valorLectura(lectura.getValorLectura())
                .consumoM3(lectura.getConsumoM3())
                .fechaLectura(lectura.getFechaLectura())
                .estado(lectura.getEstado())
                .observacion(lectura.getObservacion())
                .build();
    }
}