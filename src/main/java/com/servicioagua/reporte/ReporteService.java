package com.servicioagua.reporte;

import com.servicioagua.cliente.Cliente;
import com.servicioagua.common.enums.SeveridadFuga;
import com.servicioagua.factura.Factura;
import com.servicioagua.pago.PagoRepository;
import com.servicioagua.red.Fuga;
import com.servicioagua.reporte.dto.ClienteMorosoDto;
import com.servicioagua.reporte.dto.ConsumoSectorDto;
import com.servicioagua.reporte.dto.FacturaMorosaDto;
import com.servicioagua.reporte.dto.FugaActivaDto;
import com.servicioagua.reporte.dto.VentaPeriodoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final ReporteRepository reporteRepository;
    private final PagoRepository pagoRepository;

    @Transactional(readOnly = true)
    public List<ConsumoSectorDto> consumoPorSector(LocalDate desde, LocalDate hasta) {
        return reporteRepository.findConsumoPorSector(desde, hasta);
    }

    @Transactional(readOnly = true)
    public List<VentaPeriodoDto> ventasPorPeriodo(LocalDate desde, LocalDate hasta) {
        List<VentaPeriodoDto> ventas = new ArrayList<>();
        YearMonth mes = YearMonth.from(desde);
        YearMonth mesFin = YearMonth.from(hasta);
        while (!mes.isAfter(mesFin)) {
            BigDecimal total = pagoRepository.sumMontosEntreFechas(mes.atDay(1), mes.atEndOfMonth());
            ventas.add(new VentaPeriodoDto(mes.toString(), total));
            mes = mes.plusMonths(1);
        }
        return ventas;
    }

    @Transactional(readOnly = true)
    public List<ClienteMorosoDto> clientesMorosos() {
        Map<Long, List<Factura>> deudasPorCliente = reporteRepository
                .findFacturasConDeuda(LocalDate.now())
                .stream()
                .collect(Collectors.groupingBy(f -> f.getCliente().getId(),
                        LinkedHashMap::new, Collectors.toList()));

        return deudasPorCliente.values().stream().map(facturas -> {
            Cliente cliente = facturas.get(0).getCliente();
            BigDecimal deudaTotal = facturas.stream()
                    .map(Factura::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            List<FacturaMorosaDto> detalle = facturas.stream()
                    .map(this::toFacturaMorosa)
                    .toList();
            return new ClienteMorosoDto(cliente.getId(), cliente.getDocumento(), cliente.getNombre(),
                    deudaTotal, detalle.size(), detalle);
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<FugaActivaDto> fugasActivas(SeveridadFuga severidad) {
        return reporteRepository.findFugasActivas(severidad).stream()
                .map(this::toFugaActiva)
                .toList();
    }

    private FacturaMorosaDto toFacturaMorosa(Factura factura) {
        return new FacturaMorosaDto(factura.getId(), factura.getNumero(), factura.getTotal(),
                factura.getVencimiento(), factura.getPeriodoDesde(), factura.getPeriodoHasta(),
                factura.getEstado());
    }

    private FugaActivaDto toFugaActiva(Fuga fuga) {
        return new FugaActivaDto(fuga.getId(), fuga.getSector().getNombre(), fuga.getUbicacion(),
                fuga.getSeveridad(), fuga.getEstado(), fuga.getFechaReporte());
    }
}