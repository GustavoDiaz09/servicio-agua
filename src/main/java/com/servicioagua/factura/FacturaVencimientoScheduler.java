package com.servicioagua.factura;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class FacturaVencimientoScheduler {

    private final FacturaService facturaService;

    @Scheduled(cron = "${app.facturas.marcar-vencidas-cron:0 5 0 * * *}")
    public int marcarVencidas() {
        return facturaService.marcarVencidas();
    }
}