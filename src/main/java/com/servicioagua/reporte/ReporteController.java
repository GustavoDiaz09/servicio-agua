package com.servicioagua.reporte;

import com.servicioagua.common.dto.ApiResponse;
import com.servicioagua.common.enums.SeveridadFuga;
import com.servicioagua.reporte.dto.ClienteMorosoDto;
import com.servicioagua.reporte.dto.ConsumoSectorDto;
import com.servicioagua.reporte.dto.FugaActivaDto;
import com.servicioagua.reporte.dto.VentaPeriodoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/consumo-por-sector")
    public ResponseEntity<ApiResponse<List<ConsumoSectorDto>>> consumoPorSector(
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta) {
        RangoFechas rango = resolverRango(desde, hasta);
        List<ConsumoSectorDto> data = reporteService.consumoPorSector(rango.desde, rango.hasta);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/ventas-por-periodo")
    public ResponseEntity<ApiResponse<List<VentaPeriodoDto>>> ventasPorPeriodo(
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta) {
        RangoFechas rango = resolverRango(desde, hasta);
        List<VentaPeriodoDto> data = reporteService.ventasPorPeriodo(rango.desde, rango.hasta);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/clientes-morosos")
    public ResponseEntity<ApiResponse<List<ClienteMorosoDto>>> clientesMorosos() {
        List<ClienteMorosoDto> data = reporteService.clientesMorosos();
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/fugas-activas")
    public ResponseEntity<ApiResponse<List<FugaActivaDto>>> fugasActivas(
            @RequestParam(required = false) SeveridadFuga severidad) {
        List<FugaActivaDto> data = reporteService.fugasActivas(severidad);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    private RangoFechas resolverRango(LocalDate desde, LocalDate hasta) {
        LocalDate desdeResuelto = desde != null ? desde : YearMonth.now().atDay(1);
        LocalDate hastaResuelto = hasta != null ? hasta : YearMonth.now().atEndOfMonth();
        if (desdeResuelto.isAfter(hastaResuelto)) {
            throw new IllegalArgumentException("El rango de fechas no es válido: desde no puede ser posterior a hasta");
        }
        return new RangoFechas(desdeResuelto, hastaResuelto);
    }

    private record RangoFechas(LocalDate desde, LocalDate hasta) {
    }
}