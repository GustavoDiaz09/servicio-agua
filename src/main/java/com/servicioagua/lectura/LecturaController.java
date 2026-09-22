package com.servicioagua.lectura;

import com.servicioagua.common.dto.ApiResponse;
import com.servicioagua.lectura.dto.LecturaRequest;
import com.servicioagua.lectura.dto.LecturaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/lecturas")
@RequiredArgsConstructor
public class LecturaController {

    private final LecturaService lecturaService;

    @PostMapping
    public ResponseEntity<ApiResponse<LecturaResponse>> registrar(@Valid @RequestBody LecturaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Lectura registrada correctamente", lecturaService.registrar(request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LecturaResponse>> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(lecturaService.buscarPorId(id)));
    }

    @GetMapping("/medidor/{medidorId}")
    public ResponseEntity<ApiResponse<List<LecturaResponse>>> historialMedidor(@PathVariable Long medidorId) {
        return ResponseEntity.ok(ApiResponse.ok(lecturaService.historialMedidor(medidorId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LecturaResponse>>> listar(
            @RequestParam(required = false) Long medidorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(ApiResponse.ok(lecturaService.listar(medidorId, desde, hasta)));
    }

    @PutMapping("/{id}/anular")
    public ResponseEntity<ApiResponse<LecturaResponse>> anular(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Lectura anulada correctamente", lecturaService.anular(id)));
    }
}