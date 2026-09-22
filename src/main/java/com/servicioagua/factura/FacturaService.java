package com.servicioagua.factura;

import com.servicioagua.cliente.Cliente;
import com.servicioagua.cliente.ClienteRepository;
import com.servicioagua.common.enums.EstadoFactura;
import com.servicioagua.common.enums.EstadoMedidor;
import com.servicioagua.common.exception.BusinessException;
import com.servicioagua.common.exception.ResourceNotFoundException;
import com.servicioagua.factura.dto.FacturaResponse;
import com.servicioagua.factura.dto.GenerarFacturaRequest;
import com.servicioagua.lectura.Lectura;
import com.servicioagua.lectura.LecturaRepository;
import com.servicioagua.medidor.Medidor;
import com.servicioagua.medidor.MedidorRepository;
import com.servicioagua.tarifa.Tarifa;
import com.servicioagua.tarifa.TarifaRepository;
import com.servicioagua.toma.Toma;
import com.servicioagua.toma.TomaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FacturaService {

    private static final int CENTAVOS = 2;

    private final FacturaRepository facturaRepository;
    private final ClienteRepository clienteRepository;
    private final TomaRepository tomaRepository;
    private final MedidorRepository medidorRepository;
    private final LecturaRepository lecturaRepository;
    private final TarifaRepository tarifaRepository;

    @Transactional
    public FacturaResponse generar(GenerarFacturaRequest request) {
        if (request.getPeriodoHasta().isBefore(request.getPeriodoDesde())) {
            throw new IllegalArgumentException("El período hasta no puede ser anterior al período desde");
        }
        if (request.getVencimiento().isBefore(request.getPeriodoHasta())) {
            throw new IllegalArgumentException("El vencimiento no puede ser anterior al fin del período facturado");
        }
        Cliente cliente = clienteRepository.findByIdConBloqueo(request.getClienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", request.getClienteId()));

        if (facturaRepository.existsByClienteIdAndPeriodoDesdeAndEstadoNot(
                cliente.getId(), request.getPeriodoDesde(), EstadoFactura.ANULADA)) {
            throw new BusinessException("Ya existe una factura no anulada para este cliente en el período");
        }

        int consumoTotal = sumarConsumoPeriodo(cliente.getId(), request.getPeriodoDesde(), request.getPeriodoHasta());
        if (consumoTotal <= 0) {
            throw new BusinessException("No hay lecturas registradas en el período para el cliente");
        }

        List<Tarifa> tramos = buscarTramosVigentes(request);
        BigDecimal subtotal = calcularSubtotal(consumoTotal, tramos);
        BigDecimal cargoFijo = tramos.stream()
                .filter(t -> t.getDesdeM3() == 0)
                .map(Tarifa::getCargoFijo)
                .map(cargo -> cargo != null ? cargo : BigDecimal.ZERO)
                .findFirst()
                .orElse(BigDecimal.ZERO);

        Factura factura = Factura.builder()
                .numero(generarNumero(request.getPeriodoDesde()))
                .cliente(cliente)
                .consumoM3(consumoTotal)
                .subtotal(subtotal)
                .cargoFijo(cargoFijo)
                .total(subtotal.add(cargoFijo))
                .periodoDesde(request.getPeriodoDesde())
                .periodoHasta(request.getPeriodoHasta())
                .vencimiento(request.getVencimiento())
                .estado(EstadoFactura.PENDIENTE)
                .build();

        return toResponse(facturaRepository.save(factura));
    }

    @Transactional(readOnly = true)
    public FacturaResponse obtenerPorId(Long id) {
        return toResponse(buscarFactura(id));
    }

    @Transactional(readOnly = true)
    public FacturaResponse obtenerPorNumero(String numero) {
        Factura factura = facturaRepository.findByNumero(numero)
                .orElseThrow(() -> new ResourceNotFoundException("Factura", numero));
        return toResponse(factura);
    }

    @Transactional(readOnly = true)
    public List<FacturaResponse> listarPorCliente(Long clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new ResourceNotFoundException("Cliente", clienteId);
        }
        return facturaRepository.findByClienteIdOrderByPeriodoDesdeDesc(clienteId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FacturaResponse> listarPorEstado(EstadoFactura estado) {
        return facturaRepository.findByEstado(estado).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FacturaResponse anular(Long id) {
        Factura factura = buscarFactura(id);
        if (factura.estaPagada()) {
            throw new BusinessException("No se puede anular una factura pagada");
        }
        factura.setEstado(EstadoFactura.ANULADA);
        return toResponse(facturaRepository.save(factura));
    }

    @Transactional
    public int marcarVencidas() {
        List<Factura> vencidas = facturaRepository.findVencidas(LocalDate.now());
        vencidas.forEach(factura -> factura.setEstado(EstadoFactura.VENCIDA));
        facturaRepository.saveAll(vencidas);
        return vencidas.size();
    }

    private Factura buscarFactura(Long id) {
        return facturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Factura", id));
    }

    private int sumarConsumoPeriodo(Long clienteId, LocalDate desde, LocalDate hasta) {
        int total = 0;
        List<Toma> tomas = tomaRepository.findByClienteId(clienteId);
        for (Toma toma : tomas) {
            Medidor medidor = medidorRepository
                    .findByTomaIdAndEstado(toma.getId(), EstadoMedidor.ACTIVO)
                    .orElse(null);
            if (medidor == null) {
                continue;
            }
            List<Lectura> lecturas = lecturaRepository.findEntreFechas(medidor.getId(), desde, hasta);
            for (Lectura lectura : lecturas) {
                if (lectura.getConsumoM3() != null) {
                    total += lectura.getConsumoM3();
                }
            }
        }
        return total;
    }

    private List<Tarifa> buscarTramosVigentes(GenerarFacturaRequest request) {
        LocalDate fechaPromedio = request.getPeriodoDesde().plusDays(
                ChronoUnit.DAYS.between(request.getPeriodoDesde(), request.getPeriodoHasta()) / 2);
        List<Tarifa> tramos = tarifaRepository.findVigentesEn(fechaPromedio);
        if (tramos.isEmpty()) {
            tramos = tarifaRepository.findVigentesEn(request.getVencimiento());
        }
        if (tramos.isEmpty()) {
            throw new BusinessException("No hay tarifas vigentes para el período indicado");
        }
        return tramos;
    }

    private BigDecimal calcularSubtotal(int consumoTotal, List<Tarifa> tramos) {
        BigDecimal subtotal = BigDecimal.ZERO;
        int restante = consumoTotal;
        for (Tarifa tramo : tramos) {
            if (restante <= 0) {
                break;
            }
            int capacidad;
            if (tramo.getDesdeM3() == 0) {
                capacidad = tramo.getHastaM3();
            } else {
                capacidad = tramo.getHastaM3() - tramo.getDesdeM3() + 1;
            }
            int m3DelTramo = Math.min(restante, capacidad);
            subtotal = subtotal.add(tramo.getPrecioPorM3().multiply(BigDecimal.valueOf(m3DelTramo)));
            restante -= m3DelTramo;
        }
        if (restante > 0) {
            BigDecimal precioTop = tramos.get(tramos.size() - 1).getPrecioPorM3();
            subtotal = subtotal.add(precioTop.multiply(BigDecimal.valueOf(restante)));
        }
        return subtotal.setScale(CENTAVOS, RoundingMode.HALF_UP);
    }

    private String generarNumero(LocalDate periodoDesde) {
        long correlativo = facturaRepository.countByPeriodoDesde(periodoDesde) + 1;
        String numero = formatearNumero(periodoDesde, correlativo);
        while (facturaRepository.existsByNumero(numero)) {
            correlativo++;
            numero = formatearNumero(periodoDesde, correlativo);
        }
        return numero;
    }

    private String formatearNumero(LocalDate periodoDesde, long correlativo) {
        return String.format("FAC-%d-%02d-%04d",
                periodoDesde.getYear(), periodoDesde.getMonthValue(), correlativo);
    }

    private FacturaResponse toResponse(Factura factura) {
        return FacturaResponse.builder()
                .id(factura.getId())
                .numero(factura.getNumero())
                .clienteId(factura.getCliente().getId())
                .clienteNombre(factura.getCliente().getNombre())
                .consumoM3(factura.getConsumoM3())
                .subtotal(factura.getSubtotal())
                .cargoFijo(factura.getCargoFijo())
                .total(factura.getTotal())
                .periodoDesde(factura.getPeriodoDesde())
                .periodoHasta(factura.getPeriodoHasta())
                .vencimiento(factura.getVencimiento())
                .estado(factura.getEstado())
                .build();
    }
}