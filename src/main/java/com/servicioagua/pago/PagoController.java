package com.servicioagua.pago;

import com.servicioagua.common.dto.ApiResponse;
import com.servicioagua.pago.dto.PagoResponse;
import com.servicioagua.pago.dto.RegistrarPagoRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {

    private final PagoService pagoService;

    @PostMapping
    public ResponseEntity<ApiResponse<PagoResponse>> registrar(
            @Valid @RequestBody RegistrarPagoRequest request) {
        PagoResponse response = pagoService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Pago registrado exitosamente", response));
    }

    @GetMapping("/factura/{facturaId}")
    public ResponseEntity<ApiResponse<List<PagoResponse>>> listarPorFactura(@PathVariable Long facturaId) {
        return ResponseEntity.ok(ApiResponse.ok(pagoService.listarPorFactura(facturaId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PagoResponse>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(pagoService.obtenerPorId(id)));
    }
}