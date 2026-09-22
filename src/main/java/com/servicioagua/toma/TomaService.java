package com.servicioagua.toma;

import com.servicioagua.cliente.Cliente;
import com.servicioagua.cliente.ClienteRepository;
import com.servicioagua.common.enums.EstadoMedidor;
import com.servicioagua.common.enums.EstadoToma;
import com.servicioagua.common.exception.BusinessException;
import com.servicioagua.common.exception.DuplicateResourceException;
import com.servicioagua.common.exception.ResourceNotFoundException;
import com.servicioagua.lectura.LecturaRepository;
import com.servicioagua.medidor.Medidor;
import com.servicioagua.medidor.MedidorRepository;
import com.servicioagua.red.Sector;
import com.servicioagua.red.SectorRepository;
import com.servicioagua.toma.dto.TomaEstadoRequest;
import com.servicioagua.toma.dto.TomaRequest;
import com.servicioagua.toma.dto.TomaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TomaService {

    private final TomaRepository tomaRepository;
    private final SectorRepository sectorRepository;
    private final ClienteRepository clienteRepository;
    private final MedidorRepository medidorRepository;
    private final LecturaRepository lecturaRepository;

    @Transactional
    public TomaResponse crear(TomaRequest request) {
        if (tomaRepository.existsByCodigo(request.getCodigo())) {
            throw new DuplicateResourceException("Ya existe una toma con el código: " + request.getCodigo());
        }
        Sector sector = sectorRepository.findById(request.getSectorId())
                .orElseThrow(() -> new ResourceNotFoundException("el sector", request.getSectorId()));
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ResourceNotFoundException("el cliente", request.getClienteId()));

        Toma toma = Toma.builder()
                .codigo(request.getCodigo())
                .sector(sector)
                .cliente(cliente)
                .fechaAlta(LocalDate.now())
                .estado(EstadoToma.ACTIVA)
                .build();
        return toResponse(tomaRepository.save(toma));
    }

    @Transactional(readOnly = true)
    public List<TomaResponse> listarTodas() {
        return tomaRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TomaResponse> listarPorCliente(Long clienteId) {
        clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("el cliente", clienteId));
        return tomaRepository.findByClienteId(clienteId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TomaResponse> listarPorSector(Long sectorId) {
        sectorRepository.findById(sectorId)
                .orElseThrow(() -> new ResourceNotFoundException("el sector", sectorId));
        return tomaRepository.findBySectorId(sectorId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TomaResponse buscarPorId(Long id) {
        return toResponse(buscarToma(id));
    }

    @Transactional
    public TomaResponse actualizar(Long id, TomaRequest request) {
        Toma toma = buscarToma(id);
        if (!toma.getCodigo().equals(request.getCodigo()) && tomaRepository.existsByCodigo(request.getCodigo())) {
            throw new DuplicateResourceException("Ya existe una toma con el código: " + request.getCodigo());
        }
        Sector sector = sectorRepository.findById(request.getSectorId())
                .orElseThrow(() -> new ResourceNotFoundException("el sector", request.getSectorId()));
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ResourceNotFoundException("el cliente", request.getClienteId()));

        toma.setCodigo(request.getCodigo());
        toma.setSector(sector);
        toma.setCliente(cliente);
        return toResponse(tomaRepository.save(toma));
    }

    @Transactional
    public TomaResponse cambiarEstado(Long id, TomaEstadoRequest request) {
        Toma toma = buscarToma(id);
        EstadoToma destino = parsearEstado(request.getEstado());

        if (destino == EstadoToma.DADA_DE_BAJA) {
            throw new BusinessException("La toma solo puede darse de baja mediante el proceso de baja");
        }
        if (toma.getEstado() == EstadoToma.DADA_DE_BAJA) {
            throw new BusinessException("La toma está dada de baja y no puede cambiar de estado");
        }
        if (toma.getEstado() == destino) {
            throw new BusinessException("La toma ya se encuentra en estado " + destino);
        }
        if (!transicionValida(toma.getEstado(), destino)) {
            throw new BusinessException("Transición de estado no válida de " + toma.getEstado() + " a " + destino);
        }
        toma.setEstado(destino);
        return toResponse(tomaRepository.save(toma));
    }

    @Transactional
    public TomaResponse darDeBaja(Long id) {
        Toma toma = buscarToma(id);
        if (toma.getEstado() == EstadoToma.DADA_DE_BAJA) {
            throw new BusinessException("La toma ya está dada de baja");
        }
        medidorRepository.findByTomaIdAndEstado(id, EstadoMedidor.ACTIVO)
                .ifPresent(medidor -> validarSinLecturasEnMes(medidor));

        toma.setEstado(EstadoToma.DADA_DE_BAJA);
        return toResponse(tomaRepository.save(toma));
    }

    private void validarSinLecturasEnMes(Medidor medidor) {
        YearMonth mes = YearMonth.now();
        boolean tieneLecturas = !lecturaRepository.findEntreFechas(
                medidor.getId(), mes.atDay(1), mes.atEndOfMonth()).isEmpty();
        if (tieneLecturas) {
            throw new BusinessException("No se puede dar de baja la toma: tiene lecturas en el período");
        }
    }

    private boolean transicionValida(EstadoToma origen, EstadoToma destino) {
        return (origen == EstadoToma.ACTIVA && destino == EstadoToma.SUSPENDIDA)
                || (origen == EstadoToma.SUSPENDIDA && destino == EstadoToma.ACTIVA);
    }

    private EstadoToma parsearEstado(String estado) {
        try {
            return EstadoToma.valueOf(estado.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado de toma inválido: " + estado);
        }
    }

    private Toma buscarToma(Long id) {
        return tomaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("la toma", id));
    }

    private TomaResponse toResponse(Toma toma) {
        return TomaResponse.builder()
                .id(toma.getId())
                .codigo(toma.getCodigo())
                .sectorId(toma.getSector().getId())
                .sectorNombre(toma.getSector().getNombre())
                .clienteId(toma.getCliente().getId())
                .clienteNombre(toma.getCliente().getNombre())
                .fechaAlta(toma.getFechaAlta())
                .estado(toma.getEstado())
                .build();
    }
}