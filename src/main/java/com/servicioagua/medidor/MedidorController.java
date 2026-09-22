package com.servicioagua.medidor;

import com.servicioagua.common.dto.ApiResponse;
import com.servicioagua.medidor.dto.MedidorAsignacionRequest;
import com.servicioagua.medidor.dto.MedidorEstadoRequest;
import com.servicioagua.medidor.dto.MedidorRequest;
import com.servicioagua.medidor.dto.MedidorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medidores")
@RequiredArgsConstructor
public class MedidorController {

    private final MedidorService medidorService;

    @PostMapping
    public ResponseEntity<ApiResponse<MedidorResponse>> crear(@Valid @RequestBody MedidorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Medidor creado exitosamente", medidorService.crear(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MedidorResponse>>> listarTodos() {
        return ResponseEntity.ok(ApiResponse.ok(medidorService.listarTodos()));
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<ApiResponse<List<MedidorResponse>>> listarPorEstado(@PathVariable String estado) {
        return ResponseEntity.ok(ApiResponse.ok(medidorService.listarPorEstado(estado)));
    }

    @GetMapping("/toma/{tomaId}")
    public ResponseEntity<ApiResponse<List<MedidorResponse>>> listarPorToma(@PathVariable Long tomaId) {
        return ResponseEntity.ok(ApiResponse.ok(medidorService.listarPorToma(tomaId)));
    }

    @GetMapping("/serie/{numeroSerie}")
    public ResponseEntity<ApiResponse<MedidorResponse>> buscarPorSerie(@PathVariable String numeroSerie) {
        return ResponseEntity.ok(ApiResponse.ok(medidorService.buscarPorSerie(numeroSerie)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MedidorResponse>> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(medidorService.buscarPorId(id)));
    }

    @PutMapping("/{id}/asignar")
    public ResponseEntity<ApiResponse<MedidorResponse>> asignarAToma(@PathVariable Long id,
                                                                     @Valid @RequestBody MedidorAsignacionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Medidor asignado exitosamente", medidorService.asignarAToma(id, request)));
    }

    @PutMapping("/{id}/retirar")
    public ResponseEntity<ApiResponse<MedidorResponse>> retirar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Medidor retirado exitosamente", medidorService.retirar(id)));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<MedidorResponse>> cambiarEstado(@PathVariable Long id,
                                                                     @Valid @RequestBody MedidorEstadoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Estado actualizado exitosamente", medidorService.cambiarEstado(id, request)));
    }
}