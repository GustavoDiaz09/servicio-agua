package com.servicioagua.tarifa;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@Entity
@Table(name = "tarifa")
public class Tarifa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String nombre;

    @Min(0)
    @Column(name = "desde_m3", nullable = false)
    private Integer desdeM3;

    @Min(1)
    @Column(name = "hasta_m3", nullable = false)
    private Integer hastaM3;

    @NotNull
    @DecimalMin("0.0000")
    @Column(name = "precio_por_m3", nullable = false, precision = 10, scale = 4)
    private BigDecimal precioPorM3;

    @DecimalMin("0.00")
    @Column(name = "cargo_fijo", precision = 10, scale = 2)
    private BigDecimal cargoFijo = BigDecimal.ZERO;

    @NotNull
    @Column(name = "vigente_desde", nullable = false)
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    public boolean vigenteEn(LocalDate fecha) {
        return !fecha.isBefore(vigenteDesde)
                && (vigenteHasta == null || !fecha.isAfter(vigenteHasta));
    }
}