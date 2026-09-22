package com.servicioagua.pago;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    List<Pago> findByFacturaIdOrderByFechaPagoDesc(Long facturaId);

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p WHERE p.factura.id = :facturaId")
    BigDecimal sumMontosByFacturaId(@Param("facturaId") Long facturaId);

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM Pago p " +
            "WHERE p.fechaPago BETWEEN :desde AND :hasta")
    BigDecimal sumMontosEntreFechas(@Param("desde") LocalDate desde,
                                    @Param("hasta") LocalDate hasta);
}