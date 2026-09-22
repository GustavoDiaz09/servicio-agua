package com.servicioagua.red;

import com.servicioagua.common.dto.ApiResponse;
import com.servicioagua.red.dto.TuberiaRequest;
import com.servicioagua.red.dto.TuberiaResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tuberias")
@RequiredArgsConstructor
public class TuberiaController {

    private final TuberiaService tuberiaService;

    @PostMapping
    public ResponseEntity<ApiResponse<TuberiaResponse>> create(@Valid @RequestBody TuberiaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tubería creada exitosamente", tuberiaService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TuberiaResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(tuberiaService.findAll()));
    }

    @GetMapping("/sector/{sectorId}")
    public ResponseEntity<ApiResponse<List<TuberiaResponse>>> findBySector(@PathVariable Long sectorId) {
        return ResponseEntity.ok(ApiResponse.ok(tuberiaService.findBySector(sectorId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TuberiaResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(tuberiaService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TuberiaResponse>> update(@PathVariable Long id,
                                                               @Valid @RequestBody TuberiaRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Tubería actualizada exitosamente", tuberiaService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        tuberiaService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Tubería eliminada exitosamente", null));
    }
}
