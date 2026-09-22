package com.servicioagua.tarifa;

import com.servicioagua.common.exception.BusinessException;
import com.servicioagua.common.exception.DuplicateResourceException;
import com.servicioagua.common.exception.ResourceNotFoundException;
import com.servicioagua.tarifa.dto.TarifaRequest;
import com.servicioagua.tarifa.dto.TarifaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TarifaService {

    private final TarifaRepository tarifaRepository;

    @Transactional
    public TarifaResponse crear(TarifaRequest request) {
        validarRequest(request, null);
        validarSinSolape(request, null);

        Tarifa tarifa = Tarifa.builder()
                .nombre(request.getNombre().trim())
                .desdeM3(request.getDesdeM3())
                .hastaM3(request.getHastaM3())
                .precioPorM3(request.getPrecioPorM3())
                .cargoFijo(request.getCargoFijo() != null ? request.getCargoFijo() : BigDecimal.ZERO)
                .vigenteDesde(request.getVigenteDesde())
                .vigenteHasta(request.getVigenteHasta())
                .build();

        return TarifaResponse.fromEntity(tarifaRepository.save(tarifa));
    }

    @Transactional(readOnly = true)
    public List<TarifaResponse> listarTodas() {
        return tarifaRepository.findAllByOrderByVigenteDesdeAsc().stream()
                .map(TarifaResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TarifaResponse> listarVigentes(LocalDate fecha) {
        LocalDate consulta = fecha != null ? fecha : LocalDate.now();
        return tarifaRepository.findVigentesEn(consulta).stream()
                .map(TarifaResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public TarifaResponse buscarPorId(Long id) {
        return TarifaResponse.fromEntity(obtener(id));
    }

    @Transactional
    public TarifaResponse actualizar(Long id, TarifaRequest request) {
        Tarifa tarifa = obtener(id);
        validarRequest(request, tarifa);
        validarSinSolape(request, id);

        tarifa.setNombre(request.getNombre().trim());
        tarifa.setDesdeM3(request.getDesdeM3());
        tarifa.setHastaM3(request.getHastaM3());
        tarifa.setPrecioPorM3(request.getPrecioPorM3());
        tarifa.setCargoFijo(request.getCargoFijo() != null ? request.getCargoFijo() : BigDecimal.ZERO);
        tarifa.setVigenteDesde(request.getVigenteDesde());
        tarifa.setVigenteHasta(request.getVigenteHasta());

        return TarifaResponse.fromEntity(tarifaRepository.save(tarifa));
    }

    @Transactional
    public void eliminar(Long id) {
        Tarifa tarifa = obtener(id);
        tarifaRepository.delete(tarifa);
    }

    private Tarifa obtener(Long id) {
        return tarifaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("la tarifa", id));
    }

    private void validarRequest(TarifaRequest request, Tarifa tarifaActual) {
        String nombre = request.getNombre().trim();
        boolean nombreSinCambio = tarifaActual != null && tarifaActual.getNombre().equals(nombre);
        if (!nombreSinCambio && tarifaRepository.existsByNombre(nombre)) {
            throw new DuplicateResourceException("Ya existe una tarifa con el nombre: " + request.getNombre());
        }
        if (request.getDesdeM3() < 0) {
            throw new IllegalArgumentException("El tramo inicial (desdeM3) no puede ser negativo");
        }
        if (request.getHastaM3() <= request.getDesdeM3()) {
            throw new IllegalArgumentException("El tramo final (hastaM3) debe ser mayor que el tramo inicial (desdeM3)");
        }
        if (request.getPrecioPorM3().signum() < 0) {
            throw new IllegalArgumentException("El precio por m3 no puede ser negativo");
        }
        if (request.getCargoFijo() != null && request.getCargoFijo().signum() < 0) {
            throw new IllegalArgumentException("El cargo fijo no puede ser negativo");
        }
        if (request.getVigenteHasta() != null && request.getVigenteHasta().isBefore(request.getVigenteDesde())) {
            throw new IllegalArgumentException("La fecha de fin de vigencia no puede ser anterior a la fecha de inicio");
        }
    }

    private void validarSinSolape(TarifaRequest request, Long idExcluida) {
        LocalDate finNuevo = request.getVigenteHasta() != null ? request.getVigenteHasta() : LocalDate.MAX;
        for (Tarifa existente : tarifaRepository.findAll()) {
            if (existente.getId().equals(idExcluida)) {
                continue;
            }
            boolean solapaRango = existente.getDesdeM3() <= request.getHastaM3()
                    && existente.getHastaM3() >= request.getDesdeM3();
            LocalDate finExistente = existente.getVigenteHasta() != null ? existente.getVigenteHasta() : LocalDate.MAX;
            boolean solapaVigencia = !existente.getVigenteDesde().isAfter(finNuevo)
                    && !request.getVigenteDesde().isAfter(finExistente);
            if (solapaRango && solapaVigencia) {
                throw new BusinessException("El tramo de tarifa se solapa con '" + existente.getNombre()
                        + "' en el mismo rango de consumo y período de vigencia");
            }
        }
    }
}