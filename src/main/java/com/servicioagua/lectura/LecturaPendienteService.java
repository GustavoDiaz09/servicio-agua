package com.servicioagua.lectura;

import com.servicioagua.common.enums.EstadoLectura;
import com.servicioagua.factura.FacturaRepository;
import com.servicioagua.medidor.MedidorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class LecturaPendienteService {

    private final LecturaRepository lecturaRepository;
    private final MedidorRepository medidorRepository;
    private final FacturaRepository facturaRepository;

    @Transactional(readOnly = true)
    public boolean tienePendientesDeToma(Long tomaId, Long clienteId) {
        LocalDate corte = facturaRepository.maxPeriodoHastaNoAnulada(clienteId);
        return medidorRepository.findByTomaId(tomaId).stream()
                .anyMatch(medidor -> tienePendiente(medidor.getId(), corte));
    }

    @Transactional(readOnly = true)
    public boolean tienePendientesDeMedidor(Long medidorId, Long clienteId) {
        return tienePendiente(medidorId, facturaRepository.maxPeriodoHastaNoAnulada(clienteId));
    }

    private boolean tienePendiente(Long medidorId, LocalDate corte) {
        return corte == null
                ? lecturaRepository.existsByMedidorIdAndEstado(medidorId, EstadoLectura.ACTIVA)
                : lecturaRepository.existsByMedidorIdAndEstadoAndFechaLecturaGreaterThan(
                        medidorId, EstadoLectura.ACTIVA, corte);
    }
}
