package com.servicioagua.red;

import com.servicioagua.common.enums.EstadoFuga;
import com.servicioagua.common.exception.BusinessException;
import com.servicioagua.common.exception.ResourceNotFoundException;
import com.servicioagua.red.dto.FugaRequest;
import com.servicioagua.red.dto.FugaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FugaService {

    private final FugaRepository fugaRepository;
    private final SectorRepository sectorRepository;

    @Transactional
    public FugaResponse reportar(FugaRequest request) {
        Sector sector = sectorRepository.findById(request.getSectorId())
                .orElseThrow(() -> new ResourceNotFoundException("Sector", request.getSectorId()));
        Fuga fuga = Fuga.builder()
                .sector(sector)
                .ubicacion(request.getUbicacion())
                .severidad(request.getSeveridad())
                .estado(EstadoFuga.REPORTADA)
                .fechaReporte(LocalDate.now())
                .build();
        return toResponse(fugaRepository.save(fuga));
    }

    @Transactional(readOnly = true)
    public List<FugaResponse> findAll(EstadoFuga estado) {
        List<Fuga> fugas = estado != null
                ? fugaRepository.findByEstado(estado)
                : fugaRepository.findAll();
        return fugas.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<FugaResponse> findBySector(Long sectorId) {
        if (!sectorRepository.existsById(sectorId)) {
            throw new ResourceNotFoundException("Sector", sectorId);
        }
        return fugaRepository.findBySectorId(sectorId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public FugaResponse findById(Long id) {
        return toResponse(getFuga(id));
    }

    @Transactional
    public FugaResponse pasarEnCurso(Long id) {
        Fuga fuga = getFuga(id);
        if (fuga.getEstado() == EstadoFuga.RESUELTA) {
            throw new BusinessException("No se puede pasar a en curso una fuga resuelta");
        }
        fuga.setEstado(EstadoFuga.EN_CURSO);
        return toResponse(fugaRepository.save(fuga));
    }

    @Transactional
    public FugaResponse resolver(Long id) {
        Fuga fuga = getFuga(id);
        if (fuga.getEstado() == EstadoFuga.RESUELTA) {
            throw new BusinessException("La fuga ya se encuentra resuelta");
        }
        fuga.setEstado(EstadoFuga.RESUELTA);
        fuga.setFechaResolucion(LocalDate.now());
        return toResponse(fugaRepository.save(fuga));
    }

    private Fuga getFuga(Long id) {
        return fugaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fuga", id));
    }

    private FugaResponse toResponse(Fuga fuga) {
        return FugaResponse.builder()
                .id(fuga.getId())
                .sectorId(fuga.getSector().getId())
                .sectorNombre(fuga.getSector().getNombre())
                .ubicacion(fuga.getUbicacion())
                .severidad(fuga.getSeveridad())
                .estado(fuga.getEstado())
                .fechaReporte(fuga.getFechaReporte())
                .fechaResolucion(fuga.getFechaResolucion())
                .build();
    }
}
