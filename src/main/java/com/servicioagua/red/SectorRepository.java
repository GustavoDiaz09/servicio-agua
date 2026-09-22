package com.servicioagua.red;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SectorRepository extends JpaRepository<Sector, Long> {

    boolean existsByNombre(String nombre);
}