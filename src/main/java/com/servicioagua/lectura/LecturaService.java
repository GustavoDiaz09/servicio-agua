package com.servicioagua.lectura;

import com.servicioagua.common.enums.EstadoLectura;
import com.servicioagua.common.exception.BusinessException;
import com.servicioagua.common.exception.ResourceNotFoundException;
import com.servicioagua.lectura.dto.LecturaRequest;
import com.servicioagua.lectura.dto.LecturaResponse;
import com.servicioagua.medidor.Medidor;
import com.servicioagua.medidor.MedidorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LecturaService {

    private final LecturaRepository lecturaRepository;
    private final MedidorRepository medidorRepository;

    @Transactional
    public LecturaResponse registrar(LecturaRequest request) {
        Medidor medidor = medidorRepository.findById(request.getMedidorId())
                .orElseThrow(() -> new ResourceNotFoundException("el medidor", request.getMedidorId()));

        if (lecturaRepository.existsByMedidorIdAndFechaLecturaAndEstadoNot(
                request.getMedidorId(), request.getFechaLectura(), EstadoLectura.ANULADA)) {
            throw new BusinessException("Ya existe una lectura activa registrada para este medidor en esa fecha");
        }

        Integer anterior = lecturaRepository
                .findTopByMedidorIdAndEstadoOrderByFechaLecturaDesc(request.getMedidorId(), EstadoLectura.ACTIVA)
                .map(Lectura::getValorLectura)
                .orElseGet(() -> medidor.getLecturaAnteriorAjuste() != null
                        ? medidor.getLecturaAnteriorAjuste()
                        : 0);

        Integer consumo = request.getValorLectura() - anterior;
        if (consumo < 0) {
            throw new BusinessException("El consumo no puede ser negativo; verifique la lectura");
        }

        Lectura lectura = Lectura.builder()
                .medidor(medidor)
                .valorLectura(request.getValorLectura())
                .consumoM3(consumo)
                .fechaLectura(request.getFechaLectura())
                .estado(EstadoLectura.ACTIVA)
                .observacion(request.getObservacion())
                .build();

        return LecturaResponse.fromEntity(lecturaRepository.save(lectura));
    }

    @Transactional(readOnly = true)
    public LecturaResponse buscarPorId(Long id) {
        return LecturaResponse.fromEntity(obtener(id));
    }

    @Transactional(readOnly = true)
    public List<LecturaResponse> historialMedidor(Long medidorId) {
        verificarMedidor(medidorId);
        return lecturaRepository.findByMedidorIdOrderByFechaLecturaDesc(medidorId).stream()
                .map(LecturaResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LecturaResponse> listar(Long medidorId, LocalDate desde, LocalDate hasta) {
        boolean filtrarPorFechas = desde != null || hasta != null;
        if (filtrarPorFechas) {
            if (medidorId == null) {
                throw new IllegalArgumentException("El parámetro 'medidorId' es obligatorio para filtrar por rango de fechas");
            }
            return listarPorRango(medidorId, desde, hasta);
        }
        if (medidorId != null) {
            return historialMedidor(medidorId);
        }
        return listarTodas();
    }

    @Transactional(readOnly = true)
    public List<LecturaResponse> listarPorRango(Long medidorId, LocalDate desde, LocalDate hasta) {
        verificarMedidor(medidorId);
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Debe indicar 'desde' y 'hasta' para filtrar por rango de fechas");
        }
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a la fecha 'hasta'");
        }
        return lecturaRepository.findEntreFechas(medidorId, desde, hasta).stream()
                .map(LecturaResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LecturaResponse> listarTodas() {
        return lecturaRepository.findAll(Sort.by(Sort.Direction.DESC, "fechaLectura")).stream()
                .map(LecturaResponse::fromEntity)
                .toList();
    }

    @Transactional
    public LecturaResponse anular(Long id) {
        Lectura lectura = obtener(id);
        if (lectura.getEstado() != EstadoLectura.ACTIVA) {
            throw new BusinessException("Solo se pueden anular lecturas activas");
        }
        lectura.setEstado(EstadoLectura.ANULADA);
        return LecturaResponse.fromEntity(lecturaRepository.save(lectura));
    }

    private Lectura obtener(Long id) {
        return lecturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("la lectura", id));
    }

    private void verificarMedidor(Long medidorId) {
        if (!medidorRepository.existsById(medidorId)) {
            throw new ResourceNotFoundException("el medidor", medidorId);
        }
    }
}