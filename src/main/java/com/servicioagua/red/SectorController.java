package com.servicioagua.red;

import com.servicioagua.common.dto.ApiResponse;
import com.servicioagua.red.dto.SectorRequest;
import com.servicioagua.red.dto.SectorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sectores")
@RequiredArgsConstructor
public class SectorController {

    private final SectorService sectorService;

    @PostMapping
    public ResponseEntity<ApiResponse<SectorResponse>> create(@Valid @RequestBody SectorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Sector creado exitosamente", sectorService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SectorResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(sectorService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SectorResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(sectorService.findById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SectorResponse>> update(@PathVariable Long id,
                                                              @Valid @RequestBody SectorRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Sector actualizado exitosamente", sectorService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        sectorService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Sector eliminado exitosamente", null));
    }
}
