package com.servicioagua.lectura;

import com.servicioagua.common.enums.EstadoLectura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LecturaRepository extends JpaRepository<Lectura, Long> {

    Optional<Lectura> findTopByMedidorIdAndEstadoOrderByFechaLecturaDesc(
            Long medidorId, EstadoLectura estado);

    List<Lectura> findByMedidorIdOrderByFechaLecturaDesc(Long medidorId);

    @Query("SELECT l FROM Lectura l WHERE l.medidor.id = :medidorId " +
            "AND l.fechaLectura BETWEEN :desde AND :hasta " +
            "AND l.estado <> 'ANULADA' ORDER BY l.fechaLectura ASC")
    List<Lectura> findEntreFechas(@Param("medidorId") Long medidorId,
                                  @Param("desde") LocalDate desde,
                                  @Param("hasta") LocalDate hasta);

    boolean existsByMedidorIdAndFechaLecturaAndEstadoNot(
            Long medidorId, LocalDate fechaLectura, EstadoLectura estado);
}