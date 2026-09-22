package com.servicioagua.pago;

import com.servicioagua.common.enums.EstadoFactura;
import com.servicioagua.common.exception.BusinessException;
import com.servicioagua.common.exception.ResourceNotFoundException;
import com.servicioagua.factura.Factura;
import com.servicioagua.factura.FacturaRepository;
import com.servicioagua.pago.dto.PagoResponse;
import com.servicioagua.pago.dto.RegistrarPagoRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PagoService {

    private final PagoRepository pagoRepository;
    private final FacturaRepository facturaRepository;

    @Transactional
    public PagoResponse registrar(RegistrarPagoRequest request) {
        Factura factura = facturaRepository.findByIdConBloqueo(request.getFacturaId())
                .orElseThrow(() -> new ResourceNotFoundException("Factura", request.getFacturaId()));

        if (factura.estaPagada()) {
            throw new BusinessException("La factura está pagada o anulada y no admite nuevos pagos");
        }

        if (request.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a 0");
        }

        BigDecimal totalPagado = pagoRepository.sumMontosByFacturaId(factura.getId());
        BigDecimal deudaPendiente = factura.getTotal().subtract(totalPagado);
        if (request.getMonto().compareTo(deudaPendiente) > 0) {
            throw new IllegalArgumentException("El monto supera la deuda pendiente de la factura");
        }

        Pago pago = Pago.builder()
                .factura(factura)
                .monto(request.getMonto())
                .fechaPago(LocalDate.now())
                .metodo(request.getMetodo())
                .referencia(request.getReferencia())
                .build();
        pagoRepository.save(pago);

        BigDecimal totalPagadoActualizado = pagoRepository.sumMontosByFacturaId(factura.getId());
        if (totalPagadoActualizado.compareTo(factura.getTotal()) >= 0) {
            factura.setEstado(EstadoFactura.PAGADA);
        } else if (totalPagadoActualizado.compareTo(BigDecimal.ZERO) > 0) {
            factura.setEstado(EstadoFactura.PARCIAL);
        }
        facturaRepository.save(factura);

        return toResponse(pago, factura.getEstado());
    }

    @Transactional(readOnly = true)
    public PagoResponse obtenerPorId(Long id) {
        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", id));
        return toResponse(pago, pago.getFactura().getEstado());
    }

    @Transactional(readOnly = true)
    public List<PagoResponse> listarPorFactura(Long facturaId) {
        Factura factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Factura", facturaId));
        return pagoRepository.findByFacturaIdOrderByFechaPagoDesc(facturaId).stream()
                .map(pago -> toResponse(pago, factura.getEstado()))
                .toList();
    }

    private PagoResponse toResponse(Pago pago, EstadoFactura estadoFactura) {
        return PagoResponse.builder()
                .id(pago.getId())
                .facturaId(pago.getFactura().getId())
                .facturaNumero(pago.getFactura().getNumero())
                .monto(pago.getMonto())
                .fechaPago(pago.getFechaPago())
                .metodo(pago.getMetodo())
                .referencia(pago.getReferencia())
                .estadoFactura(estadoFactura)
                .build();
    }
}