package com.servicioagua.factura;

import com.servicioagua.common.enums.EstadoFactura;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    boolean existsByClienteIdAndPeriodoDesdeAndEstadoNot(Long clienteId, LocalDate periodoDesde, EstadoFactura estado);

    @Query("SELECT MAX(f.periodoHasta) FROM Factura f " +
            "WHERE f.cliente.id = :clienteId AND f.estado <> 'ANULADA'")
    LocalDate maxPeriodoHastaNoAnulada(@Param("clienteId") Long clienteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Factura f WHERE f.id = :id")
    java.util.Optional<Factura> findByIdConBloqueo(@Param("id") Long id);
}