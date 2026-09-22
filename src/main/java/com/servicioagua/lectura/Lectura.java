package com.servicioagua.lectura;

import com.servicioagua.common.enums.EstadoLectura;
import com.servicioagua.medidor.Medidor;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
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
@Table(name = "lectura")
public class Lectura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medidor_id", nullable = false)
    private Medidor medidor;

    @NotNull
    @Min(0)
    @Column(name = "valor_lectura", nullable = false)
    private Integer valorLectura;

    @Column(name = "consumo_m3")
    private Integer consumoM3;

    @NotNull
    @Column(name = "fecha_lectura", nullable = false)
    private LocalDate fechaLectura;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoLectura estado = EstadoLectura.ACTIVA;

    @Column(length = 255)
    private String observacion;
}