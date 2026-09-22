package com.servicioagua.red;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TuberiaRepository extends JpaRepository<Tuberia, Long> {

    List<Tuberia> findBySectorId(Long sectorId);
}