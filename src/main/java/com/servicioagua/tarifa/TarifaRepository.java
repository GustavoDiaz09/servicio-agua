package com.servicioagua.tarifa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TarifaRepository extends JpaRepository<Tarifa, Long> {

    @Query("SELECT t FROM Tarifa t WHERE t.vigenteDesde <= :fecha " +
            "AND (t.vigenteHasta IS NULL OR t.vigenteHasta >= :fecha) " +
            "ORDER BY t.desdeM3 ASC")
    List<Tarifa> findVigentesEn(@Param("fecha") LocalDate fecha);

    List<Tarifa> findAllByOrderByVigenteDesdeAsc();

    boolean existsByNombre(String nombre);
}