package com.servicioagua.reporte;

import com.servicioagua.common.enums.SeveridadFuga;
import com.servicioagua.factura.Factura;
import com.servicioagua.lectura.Lectura;
import com.servicioagua.red.Fuga;
import com.servicioagua.reporte.dto.ConsumoSectorDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReporteRepository extends JpaRepository<Lectura, Long> {

    @Query("SELECT new com.servicioagua.reporte.dto.ConsumoSectorDto(s.id, s.nombre, COALESCE(SUM(l.consumoM3), 0L)) " +
            "FROM Lectura l JOIN l.medidor m JOIN m.toma t JOIN t.sector s " +
            "WHERE l.estado IN ('ACTIVA', 'ESTIMADA') AND l.fechaLectura BETWEEN :desde AND :hasta " +
            "GROUP BY s.id, s.nombre ORDER BY s.nombre")
    List<ConsumoSectorDto> findConsumoPorSector(@Param("desde") LocalDate desde,
                                                @Param("hasta") LocalDate hasta);

    @Query("SELECT f FROM Factura f WHERE f.estado IN ('PENDIENTE', 'PARCIAL', 'VENCIDA') " +
            "AND (f.estado = 'VENCIDA' OR f.vencimiento < :fecha) ORDER BY f.cliente.id, f.vencimiento")
    List<Factura> findFacturasConDeuda(@Param("fecha") LocalDate fecha);

    @Query("SELECT f FROM Fuga f WHERE f.estado IN ('REPORTADA', 'EN_CURSO') " +
            "AND (:severidad IS NULL OR f.severidad = :severidad) ORDER BY f.fechaReporte")
    List<Fuga> findFugasActivas(@Param("severidad") SeveridadFuga severidad);
}