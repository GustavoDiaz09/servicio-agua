package com.servicioagua.factura;

import com.servicioagua.cliente.Cliente;
import com.servicioagua.common.enums.EstadoFactura;
import com.servicioagua.lectura.Lectura;
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
@Table(name = "factura", uniqueConstraints = @UniqueConstraint(columnNames = "numero"))
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lectura_id")
    private Lectura lectura;

    @NotNull
    @Column(name = "consumo_m3", nullable = false)
    private Integer consumoM3;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "cargo_fijo", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal cargoFijo = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @NotNull
    @Column(name = "periodo_desde", nullable = false)
    private LocalDate periodoDesde;

    @NotNull
    @Column(name = "periodo_hasta", nullable = false)
    private LocalDate periodoHasta;

    @NotNull
    @Column(nullable = false)
    private LocalDate vencimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoFactura estado = EstadoFactura.PENDIENTE;

    public boolean estaPagada() {
        return EstadoFactura.PAGADA.equals(estado) || EstadoFactura.ANULADA.equals(estado);
    }
}