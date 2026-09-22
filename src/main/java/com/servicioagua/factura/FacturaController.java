package com.servicioagua.factura;

import com.servicioagua.common.dto.ApiResponse;
import com.servicioagua.common.enums.EstadoFactura;
import com.servicioagua.factura.dto.FacturaResponse;
import com.servicioagua.factura.dto.GenerarFacturaRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/facturas")
@RequiredArgsConstructor
public class FacturaController {

    private final FacturaService facturaService;

    @PostMapping("/generar")
    public ResponseEntity<ApiResponse<FacturaResponse>> generar(
            @Valid @RequestBody GenerarFacturaRequest request) {
        FacturaResponse response = facturaService.generar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Factura generada exitosamente", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FacturaResponse>> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(facturaService.obtenerPorId(id)));
    }

    @GetMapping("/numero/{numero}")
    public ResponseEntity<ApiResponse<FacturaResponse>> obtenerPorNumero(@PathVariable String numero) {
        return ResponseEntity.ok(ApiResponse.ok(facturaService.obtenerPorNumero(numero)));
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<ApiResponse<List<FacturaResponse>>> listarPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(ApiResponse.ok(facturaService.listarPorCliente(clienteId)));
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<ApiResponse<List<FacturaResponse>>> listarPorEstado(
            @PathVariable EstadoFactura estado) {
        return ResponseEntity.ok(ApiResponse.ok(facturaService.listarPorEstado(estado)));
    }

    @PutMapping("/{id}/anular")
    public ResponseEntity<ApiResponse<FacturaResponse>> anular(@PathVariable Long id) {
        FacturaResponse response = facturaService.anular(id);
        return ResponseEntity.ok(ApiResponse.ok("Factura anulada exitosamente", response));
    }

    @PostMapping("/marcar-vencidas")
    public ResponseEntity<ApiResponse<Integer>> marcarVencidas() {
        int cantidad = facturaService.marcarVencidas();
        return ResponseEntity.ok(ApiResponse.ok("Facturas vencidas marcadas exitosamente", cantidad));
    }
}