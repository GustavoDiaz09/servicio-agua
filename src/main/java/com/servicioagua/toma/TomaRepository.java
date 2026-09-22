package com.servicioagua.toma;

import com.servicioagua.cliente.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TomaRepository extends JpaRepository<Toma, Long> {

    boolean existsByCodigo(String codigo);

    Optional<Toma> findByCodigo(String codigo);

    List<Toma> findByClienteId(Long clienteId);

    List<Toma> findBySectorId(Long sectorId);
}