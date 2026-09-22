package com.servicioagua.medidor;

import com.servicioagua.common.enums.EstadoMedidor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedidorRepository extends JpaRepository<Medidor, Long> {

    boolean existsByNumeroSerie(String numeroSerie);

    Optional<Medidor> findByNumeroSerie(String numeroSerie);

    List<Medidor> findByTomaId(Long tomaId);

    Optional<Medidor> findByTomaIdAndEstado(Long tomaId, EstadoMedidor estado);

    List<Medidor> findByEstado(EstadoMedidor estado);
}