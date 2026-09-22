package com.servicioagua.red;

import com.servicioagua.common.enums.EstadoFuga;
import com.servicioagua.common.enums.SeveridadFuga;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fuga")
public class Fuga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String ubicacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeveridadFuga severidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoFuga estado = EstadoFuga.REPORTADA;

    @NotNull
    @Column(name = "fecha_reporte", nullable = false)
    @Builder.Default
    private LocalDate fechaReporte = LocalDate.now();

    @Column(name = "fecha_resolucion")
    private LocalDate fechaResolucion;
}