package com.servicioagua.factura;

import com.servicioagua.common.enums.EstadoFactura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

    Optional<Factura> findByNumero(String numero);

    boolean existsByNumero(String numero);

    List<Factura> findByClienteIdOrderByPeriodoDesdeDesc(Long clienteId);

    List<Factura> findByEstado(EstadoFactura estado);

    @Query("SELECT f FROM Factura f WHERE f.estado IN ('PENDIENTE', 'PARCIAL') " +
            "AND f.vencimiento < :fecha")
    List<Factura> findVencidas(@Param("fecha") LocalDate fecha);

    long countByPeriodoDesde(LocalDate periodoDesde);

    boolean existsByClienteIdAndPeriodoDesde(Long clienteId, LocalDate periodoDesde);
}