package com.servicioagua.red;

import jakarta.persistence.*;
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
@Table(name = "tuberia")
public class Tuberia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @jakarta.validation.constraints.NotBlank
    @Column(nullable = false, length = 60)
    private String material;

    @Column(name = "diametro_mm", precision = 8, scale = 2)
    private BigDecimal diametroMm;

    @Column(name = "longitud_m", precision = 12, scale = 2)
    private BigDecimal longitudM;

    @Column(length = 100)
    private String tramo;

    @NotNull
    @Column(name = "fecha_instalacion", nullable = false)
    @Builder.Default
    private LocalDate fechaInstalacion = LocalDate.now();
}