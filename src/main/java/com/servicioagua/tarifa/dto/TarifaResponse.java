package com.servicioagua.tarifa.dto;

import com.servicioagua.tarifa.Tarifa;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class TarifaResponse {

    private Long id;
    private String nombre;
    private Integer desdeM3;
    private Integer hastaM3;
    private BigDecimal precioPorM3;
    private BigDecimal cargoFijo;
    private LocalDate vigenteDesde;
    private LocalDate vigenteHasta;

    public static TarifaResponse fromEntity(Tarifa tarifa) {
        return TarifaResponse.builder()
                .id(tarifa.getId())
                .nombre(tarifa.getNombre())
                .desdeM3(tarifa.getDesdeM3())
                .hastaM3(tarifa.getHastaM3())
                .precioPorM3(tarifa.getPrecioPorM3())
                .cargoFijo(tarifa.getCargoFijo())
                .vigenteDesde(tarifa.getVigenteDesde())
                .vigenteHasta(tarifa.getVigenteHasta())
                .build();
    }
}