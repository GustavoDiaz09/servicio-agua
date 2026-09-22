package com.servicioagua.medidor;

import com.servicioagua.common.enums.EstadoLectura;
import com.servicioagua.common.enums.EstadoMedidor;
import com.servicioagua.common.enums.EstadoToma;
import com.servicioagua.common.exception.BusinessException;
import com.servicioagua.common.exception.DuplicateResourceException;
import com.servicioagua.common.exception.ResourceNotFoundException;
import com.servicioagua.factura.FacturaRepository;
import com.servicioagua.lectura.LecturaRepository;
import com.servicioagua.medidor.dto.MedidorAsignacionRequest;
import com.servicioagua.medidor.dto.MedidorEstadoRequest;
import com.servicioagua.medidor.dto.MedidorRequest;
import com.servicioagua.medidor.dto.MedidorResponse;
import com.servicioagua.toma.Toma;
import com.servicioagua.toma.TomaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedidorService {

    private final MedidorRepository medidorRepository;
    private final TomaRepository tomaRepository;
    private final LecturaRepository lecturaRepository;
    private final FacturaRepository facturaRepository;

    @Transactional
    public MedidorResponse crear(MedidorRequest request) {
        if (medidorRepository.existsByNumeroSerie(request.getNumeroSerie())) {
            throw new DuplicateResourceException("Ya existe un medidor con el número de serie: " + request.getNumeroSerie());
        }
        Medidor medidor = Medidor.builder()
                .numeroSerie(request.getNumeroSerie())
                .lecturaAnteriorAjuste(request.getLecturaAnteriorAjuste())
                .fechaInstalacion(LocalDate.now())
                .estado(EstadoMedidor.ACTIVO)
                .build();
        return toResponse(medidorRepository.save(medidor));
    }

    @Transactional(readOnly = true)
    public List<MedidorResponse> listarTodos() {
        return medidorRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MedidorResponse> listarPorEstado(String estado) {
        return medidorRepository.findByEstado(parsearEstado(estado)).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MedidorResponse> listarPorToma(Long tomaId) {
        tomaRepository.findById(tomaId)
                .orElseThrow(() -> new ResourceNotFoundException("la toma", tomaId));
        return medidorRepository.findByTomaId(tomaId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MedidorResponse buscarPorId(Long id) {
        return toResponse(buscarMedidor(id));
    }

    @Transactional(readOnly = true)
    public MedidorResponse buscarPorSerie(String numeroSerie) {
        Medidor medidor = medidorRepository.findByNumeroSerie(numeroSerie)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el medidor con serie: " + numeroSerie));
        return toResponse(medidor);
    }

    @Transactional
    public MedidorResponse asignarAToma(Long id, MedidorAsignacionRequest request) {
        Medidor medidor = buscarMedidor(id);
        Toma toma = tomaRepository.findById(request.getTomaId())
                .orElseThrow(() -> new ResourceNotFoundException("la toma", request.getTomaId()));
        if (toma.getEstado() != EstadoToma.ACTIVA) {
            throw new BusinessException("La toma debe estar ACTIVA para asignar un medidor");
        }
        medidorRepository.findByTomaIdAndEstado(toma.getId(), EstadoMedidor.ACTIVO)
                .filter(activo -> !activo.getId().equals(medidor.getId()))
                .ifPresent(activo -> {
                    throw new BusinessException("La toma ya tiene un medidor activo");
                });

        medidor.setToma(toma);
        medidor.setEstado(EstadoMedidor.ACTIVO);
        medidor.setFechaInstalacion(LocalDate.now());
        return toResponse(medidorRepository.save(medidor));
    }

    @Transactional
    public MedidorResponse retirar(Long id) {
        Medidor medidor = buscarMedidor(id);
        if (medidor.getEstado() == EstadoMedidor.RETIRADO) {
            throw new BusinessException("El medidor ya está retirado");
        }
        Toma toma = medidor.getToma();
        if (toma != null) {
            LocalDate corte = facturaRepository.maxPeriodoHastaNoAnulada(toma.getCliente().getId());
            boolean tienePendientes = corte == null
                    ? lecturaRepository.existsByMedidorIdAndEstado(medidor.getId(), EstadoLectura.ACTIVA)
                    : lecturaRepository.existsByMedidorIdAndEstadoAndFechaLecturaGreaterThan(
                            medidor.getId(), EstadoLectura.ACTIVA, corte);
            if (tienePendientes) {
                throw new BusinessException("No se puede retirar el medidor: tiene lecturas sin facturar");
            }
        }
        medidor.setEstado(EstadoMedidor.RETIRADO);
        medidor.setToma(null);
        return toResponse(medidorRepository.save(medidor));
    }

    @Transactional
    public MedidorResponse cambiarEstado(Long id, MedidorEstadoRequest request) {
        Medidor medidor = buscarMedidor(id);
        EstadoMedidor destino = parsearEstado(request.getEstado());
        if (destino == EstadoMedidor.RETIRADO) {
            throw new BusinessException("El estado RETIRADO se gestiona mediante el proceso de retiro");
        }
        if (medidor.getEstado() == EstadoMedidor.RETIRADO) {
            throw new BusinessException("El medidor está retirado y no puede cambiar de estado");
        }
        if (medidor.getEstado() == destino) {
            throw new BusinessException("El medidor ya se encuentra en estado " + destino);
        }
        medidor.setEstado(destino);
        return toResponse(medidorRepository.save(medidor));
    }

    private EstadoMedidor parsearEstado(String estado) {
        try {
            return EstadoMedidor.valueOf(estado.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado de medidor inválido: " + estado);
        }
    }

    private Medidor buscarMedidor(Long id) {
        return medidorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("el medidor", id));
    }

    private MedidorResponse toResponse(Medidor medidor) {
        Toma toma = medidor.getToma();
        return MedidorResponse.builder()
                .id(medidor.getId())
                .numeroSerie(medidor.getNumeroSerie())
                .tomaId(toma != null ? toma.getId() : null)
                .tomaCodigo(toma != null ? toma.getCodigo() : null)
                .lecturaAnteriorAjuste(medidor.getLecturaAnteriorAjuste())
                .fechaInstalacion(medidor.getFechaInstalacion())
                .estado(medidor.getEstado())
                .build();
    }
}