package com.servicioagua.medidor;

import com.servicioagua.common.enums.EstadoMedidor;
import com.servicioagua.toma.Toma;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
@Table(name = "medidor", uniqueConstraints = @UniqueConstraint(columnNames = "numero_serie"))
public class Medidor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "numero_serie", nullable = false, length = 50)
    private String numeroSerie;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "toma_id")
    private Toma toma;

    @Column(name = "lectura_anterior_ajuste")
    private Integer lecturaAnteriorAjuste;

    @Column(nullable = false)
    @Builder.Default
    private LocalDate fechaInstalacion = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoMedidor estado = EstadoMedidor.ACTIVO;
}