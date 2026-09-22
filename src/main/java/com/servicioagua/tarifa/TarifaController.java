package com.servicioagua.tarifa;

import com.servicioagua.common.dto.ApiResponse;
import com.servicioagua.tarifa.dto.TarifaRequest;
import com.servicioagua.tarifa.dto.TarifaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/tarifas")
@RequiredArgsConstructor
public class TarifaController {

    private final TarifaService tarifaService;

    @PostMapping
    public ResponseEntity<ApiResponse<TarifaResponse>> crear(@Valid @RequestBody TarifaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tarifa creada correctamente", tarifaService.crear(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TarifaResponse>>> listarTodas() {
        return ResponseEntity.ok(ApiResponse.ok(tarifaService.listarTodas()));
    }

    @GetMapping("/vigentes")
    public ResponseEntity<ApiResponse<List<TarifaResponse>>> listarVigentes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(ApiResponse.ok(tarifaService.listarVigentes(fecha)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TarifaResponse>> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(tarifaService.buscarPorId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TarifaResponse>> actualizar(@PathVariable Long id,
                                                                  @Valid @RequestBody TarifaRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Tarifa actualizada correctamente", tarifaService.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        tarifaService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Tarifa eliminada correctamente", null));
    }
}