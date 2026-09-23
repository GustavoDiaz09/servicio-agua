package com.servicioagua.red;

import com.servicioagua.common.dto.ApiResponse;
import com.servicioagua.common.enums.EstadoMantenimiento;
import com.servicioagua.red.dto.MantenimientoEstadoRequest;
import com.servicioagua.red.dto.MantenimientoRequest;
import com.servicioagua.red.dto.MantenimientoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mantenimientos")
@RequiredArgsConstructor
public class MantenimientoController {

    private final MantenimientoService mantenimientoService;

    @PostMapping
    public ResponseEntity<ApiResponse<MantenimientoResponse>> create(
            @Valid @RequestBody MantenimientoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Mantenimiento creado exitosamente", mantenimientoService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MantenimientoResponse>>> findAll(
            @RequestParam(required = false) EstadoMantenimiento estado) {
        return ResponseEntity.ok(ApiResponse.ok(mantenimientoService.findAll(estado)));
    }

    @GetMapping("/sector/{sectorId}")
    public ResponseEntity<ApiResponse<List<MantenimientoResponse>>> findBySector(@PathVariable Long sectorId) {
        return ResponseEntity.ok(ApiResponse.ok(mantenimientoService.findBySector(sectorId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MantenimientoResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(mantenimientoService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MantenimientoResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody MantenimientoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Mantenimiento actualizado exitosamente",
                mantenimientoService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        mantenimientoService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Mantenimiento eliminado exitosamente", null));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<MantenimientoResponse>> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody MantenimientoEstadoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Estado de mantenimiento actualizado",
                mantenimientoService.cambiarEstado(id, request.getEstado())));
    }
}
