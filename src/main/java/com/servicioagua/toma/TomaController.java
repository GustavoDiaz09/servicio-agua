package com.servicioagua.toma;

import com.servicioagua.common.dto.ApiResponse;
import com.servicioagua.toma.dto.TomaEstadoRequest;
import com.servicioagua.toma.dto.TomaRequest;
import com.servicioagua.toma.dto.TomaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tomas")
@RequiredArgsConstructor
public class TomaController {

    private final TomaService tomaService;

    @PostMapping
    public ResponseEntity<ApiResponse<TomaResponse>> crear(@Valid @RequestBody TomaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Toma creada exitosamente", tomaService.crear(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TomaResponse>>> listarTodas() {
        return ResponseEntity.ok(ApiResponse.ok(tomaService.listarTodas()));
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<ApiResponse<List<TomaResponse>>> listarPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(ApiResponse.ok(tomaService.listarPorCliente(clienteId)));
    }

    @GetMapping("/sector/{sectorId}")
    public ResponseEntity<ApiResponse<List<TomaResponse>>> listarPorSector(@PathVariable Long sectorId) {
        return ResponseEntity.ok(ApiResponse.ok(tomaService.listarPorSector(sectorId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TomaResponse>> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(tomaService.buscarPorId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TomaResponse>> actualizar(@PathVariable Long id,
                                                                @Valid @RequestBody TomaRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Toma actualizada exitosamente", tomaService.actualizar(id, request)));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<TomaResponse>> cambiarEstado(@PathVariable Long id,
                                                                   @Valid @RequestBody TomaEstadoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Estado actualizado exitosamente", tomaService.cambiarEstado(id, request)));
    }

    @PutMapping("/{id}/baja")
    public ResponseEntity<ApiResponse<TomaResponse>> darDeBaja(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Toma dada de baja exitosamente", tomaService.darDeBaja(id)));
    }
}