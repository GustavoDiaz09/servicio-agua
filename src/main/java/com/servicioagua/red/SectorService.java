package com.servicioagua.red;

import com.servicioagua.common.exception.BusinessException;
import com.servicioagua.common.exception.DuplicateResourceException;
import com.servicioagua.common.exception.ResourceNotFoundException;
import com.servicioagua.red.dto.SectorRequest;
import com.servicioagua.toma.TomaRepository;
import com.servicioagua.red.dto.SectorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SectorService {

    private final SectorRepository sectorRepository;
    private final TuberiaRepository tuberiaRepository;
    private final TomaRepository tomaRepository;
    private final FugaRepository fugaRepository;
    private final MantenimientoRepository mantenimientoRepository;

    @Transactional
    public SectorResponse create(SectorRequest request) {
        if (sectorRepository.existsByNombre(request.getNombre())) {
            throw new DuplicateResourceException("Ya existe un sector con el nombre: " + request.getNombre());
        }
        Sector sector = Sector.builder()
                .nombre(request.getNombre())
                .zona(request.getZona())
                .presionPromedio(request.getPresionPromedio())
                .build();
        return toResponse(sectorRepository.save(sector));
    }

    @Transactional(readOnly = true)
    public List<SectorResponse> findAll() {
        return sectorRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SectorResponse findById(Long id) {
        return toResponse(getSector(id));
    }

    @Transactional
    public SectorResponse update(Long id, SectorRequest request) {
        Sector sector = getSector(id);
        if (!sector.getNombre().equals(request.getNombre())
                && sectorRepository.existsByNombre(request.getNombre())) {
            throw new DuplicateResourceException("Ya existe un sector con el nombre: " + request.getNombre());
        }
        sector.setNombre(request.getNombre());
        sector.setZona(request.getZona());
        sector.setPresionPromedio(request.getPresionPromedio());
        return toResponse(sectorRepository.save(sector));
    }

    @Transactional
    public void delete(Long id) {
        Sector sector = getSector(id);
        if (!tuberiaRepository.findBySectorId(id).isEmpty()
                || !tomaRepository.findBySectorId(id).isEmpty()
                || !fugaRepository.findBySectorId(id).isEmpty()
                || !mantenimientoRepository.findBySectorId(id).isEmpty()) {
            throw new BusinessException("El sector tiene elementos asociados");
        }
        sectorRepository.delete(sector);
    }

    private Sector getSector(Long id) {
        return sectorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sector", id));
    }

    private SectorResponse toResponse(Sector sector) {
        return SectorResponse.builder()
                .id(sector.getId())
                .nombre(sector.getNombre())
                .zona(sector.getZona())
                .presionPromedio(sector.getPresionPromedio())
                .build();
    }
}
