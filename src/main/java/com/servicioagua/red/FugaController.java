package com.servicioagua.red;

import com.servicioagua.common.dto.ApiResponse;
import com.servicioagua.common.enums.EstadoFuga;
import com.servicioagua.red.dto.FugaRequest;
import com.servicioagua.red.dto.FugaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fugas")
@RequiredArgsConstructor
public class FugaController {

    private final FugaService fugaService;

    @PostMapping
    public ResponseEntity<ApiResponse<FugaResponse>> reportar(@Valid @RequestBody FugaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Fuga reportada exitosamente", fugaService.reportar(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FugaResponse>>> findAll(
            @RequestParam(required = false) EstadoFuga estado) {
        return ResponseEntity.ok(ApiResponse.ok(fugaService.findAll(estado)));
    }

    @GetMapping("/sector/{sectorId}")
    public ResponseEntity<ApiResponse<List<FugaResponse>>> findBySector(@PathVariable Long sectorId) {
        return ResponseEntity.ok(ApiResponse.ok(fugaService.findBySector(sectorId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FugaResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(fugaService.findById(id)));
    }

    @PutMapping("/{id}/en-curso")
    public ResponseEntity<ApiResponse<FugaResponse>> pasarEnCurso(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Fuga en curso", fugaService.pasarEnCurso(id)));
    }

    @PutMapping("/{id}/resolver")
    public ResponseEntity<ApiResponse<FugaResponse>> resolver(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Fuga resuelta exitosamente", fugaService.resolver(id)));
    }
}
