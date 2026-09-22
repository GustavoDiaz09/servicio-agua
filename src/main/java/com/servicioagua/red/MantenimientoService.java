package com.servicioagua.red;

import com.servicioagua.common.enums.EstadoMantenimiento;
import com.servicioagua.common.exception.BusinessException;
import com.servicioagua.common.exception.ResourceNotFoundException;
import com.servicioagua.red.dto.MantenimientoRequest;
import com.servicioagua.red.dto.MantenimientoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MantenimientoService {

    private final MantenimientoRepository mantenimientoRepository;
    private final SectorRepository sectorRepository;

    @Transactional
    public MantenimientoResponse create(MantenimientoRequest request) {
        if (request.getFechaProgramada().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha programada no puede ser en el pasado");
        }
        Sector sector = sectorRepository.findById(request.getSectorId())
                .orElseThrow(() -> new ResourceNotFoundException("Sector", request.getSectorId()));
        Mantenimiento mantenimiento = Mantenimiento.builder()
                .sector(sector)
                .descripcion(request.getDescripcion())
                .tipo(request.getTipo())
                .fechaProgramada(request.getFechaProgramada())
                .estado(EstadoMantenimiento.PROGRAMADO)
                .build();
        return toResponse(mantenimientoRepository.save(mantenimiento));
    }

    @Transactional(readOnly = true)
    public List<MantenimientoResponse> findAll(EstadoMantenimiento estado) {
        List<Mantenimiento> mantenimientos = estado != null
                ? mantenimientoRepository.findByEstado(estado)
                : mantenimientoRepository.findAll();
        return mantenimientos.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MantenimientoResponse> findBySector(Long sectorId) {
        if (!sectorRepository.existsById(sectorId)) {
            throw new ResourceNotFoundException("Sector", sectorId);
        }
        return mantenimientoRepository.findBySectorId(sectorId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MantenimientoResponse findById(Long id) {
        return toResponse(getMantenimiento(id));
    }

    @Transactional
    public MantenimientoResponse cambiarEstado(Long id, EstadoMantenimiento nuevoEstado) {
        Mantenimiento mantenimiento = getMantenimiento(id);
        EstadoMantenimiento actual = mantenimiento.getEstado();

        boolean valido = (actual == EstadoMantenimiento.PROGRAMADO
                        && (nuevoEstado == EstadoMantenimiento.EN_CURSO
                        || nuevoEstado == EstadoMantenimiento.CANCELADO))
                || (actual == EstadoMantenimiento.EN_CURSO
                && nuevoEstado == EstadoMantenimiento.COMPLETADO);

        if (!valido) {
            throw new BusinessException(
                    "Transición de estado inválida: " + actual + " → " + nuevoEstado);
        }

        mantenimiento.setEstado(nuevoEstado);
        if (nuevoEstado == EstadoMantenimiento.COMPLETADO) {
            mantenimiento.setFechaFin(LocalDate.now());
        }
        return toResponse(mantenimientoRepository.save(mantenimiento));
    }

    private Mantenimiento getMantenimiento(Long id) {
        return mantenimientoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mantenimiento", id));
    }

    private MantenimientoResponse toResponse(Mantenimiento mantenimiento) {
        return MantenimientoResponse.builder()
                .id(mantenimiento.getId())
                .sectorId(mantenimiento.getSector().getId())
                .sectorNombre(mantenimiento.getSector().getNombre())
                .descripcion(mantenimiento.getDescripcion())
                .tipo(mantenimiento.getTipo())
                .fechaProgramada(mantenimiento.getFechaProgramada())
                .fechaFin(mantenimiento.getFechaFin())
                .estado(mantenimiento.getEstado())
                .build();
    }
}
