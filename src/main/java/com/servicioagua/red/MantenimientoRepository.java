package com.servicioagua.red;

import com.servicioagua.common.enums.EstadoMantenimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MantenimientoRepository extends JpaRepository<Mantenimiento, Long> {

    List<Mantenimiento> findBySectorId(Long sectorId);

    List<Mantenimiento> findByEstado(EstadoMantenimiento estado);
}