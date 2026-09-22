package com.servicioagua.red;

import com.servicioagua.common.enums.EstadoFuga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FugaRepository extends JpaRepository<Fuga, Long> {

    List<Fuga> findBySectorId(Long sectorId);

    List<Fuga> findByEstado(EstadoFuga estado);

    List<Fuga> findByEstadoNot(EstadoFuga estado);
}