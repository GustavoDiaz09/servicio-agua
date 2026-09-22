package com.servicioagua.red.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectorResponse {

    private Long id;
    private String nombre;
    private String zona;
    private BigDecimal presionPromedio;
}
