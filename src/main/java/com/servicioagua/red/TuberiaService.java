package com.servicioagua.red;

import com.servicioagua.common.exception.ResourceNotFoundException;
import com.servicioagua.red.dto.TuberiaRequest;
import com.servicioagua.red.dto.TuberiaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TuberiaService {

    private final TuberiaRepository tuberiaRepository;
    private final SectorRepository sectorRepository;

    @Transactional
    public TuberiaResponse create(TuberiaRequest request) {
        Sector sector = getSector(request.getSectorId());
        Tuberia tuberia = Tuberia.builder()
                .sector(sector)
                .material(request.getMaterial())
                .diametroMm(request.getDiametroMm())
                .longitudM(request.getLongitudM())
                .tramo(request.getTramo())
                .fechaInstalacion(request.getFechaInstalacion() != null
                        ? request.getFechaInstalacion()
                        : LocalDate.now())
                .build();
        return toResponse(tuberiaRepository.save(tuberia));
    }

    @Transactional(readOnly = true)
    public List<TuberiaResponse> findAll() {
        return tuberiaRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TuberiaResponse> findBySector(Long sectorId) {
        getSector(sectorId);
        return tuberiaRepository.findBySectorId(sectorId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TuberiaResponse findById(Long id) {
        return toResponse(getTuberia(id));
    }

    @Transactional
    public TuberiaResponse update(Long id, TuberiaRequest request) {
        Tuberia tuberia = getTuberia(id);
        tuberia.setSector(getSector(request.getSectorId()));
        tuberia.setMaterial(request.getMaterial());
        tuberia.setDiametroMm(request.getDiametroMm());
        tuberia.setLongitudM(request.getLongitudM());
        tuberia.setTramo(request.getTramo());
        tuberia.setFechaInstalacion(request.getFechaInstalacion() != null
                ? request.getFechaInstalacion()
                : tuberia.getFechaInstalacion());
        return toResponse(tuberiaRepository.save(tuberia));
    }

    @Transactional
    public void delete(Long id) {
        tuberiaRepository.delete(getTuberia(id));
    }

    private Sector getSector(Long sectorId) {
        return sectorRepository.findById(sectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Sector", sectorId));
    }

    private Tuberia getTuberia(Long id) {
        return tuberiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tubería", id));
    }

    private TuberiaResponse toResponse(Tuberia tuberia) {
        return TuberiaResponse.builder()
                .id(tuberia.getId())
                .sectorId(tuberia.getSector().getId())
                .sectorNombre(tuberia.getSector().getNombre())
                .material(tuberia.getMaterial())
                .diametroMm(tuberia.getDiametroMm())
                .longitudM(tuberia.getLongitudM())
                .tramo(tuberia.getTramo())
                .fechaInstalacion(tuberia.getFechaInstalacion())
                .build();
    }
}
