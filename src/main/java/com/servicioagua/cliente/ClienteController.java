package com.servicioagua.cliente;

import com.servicioagua.cliente.dto.ClienteRequest;
import com.servicioagua.cliente.dto.ClienteResponse;
import com.servicioagua.common.dto.ApiResponse;
import com.servicioagua.common.enums.EstadoCliente;
import com.servicioagua.common.enums.TipoCliente;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping
    public ResponseEntity<ApiResponse<ClienteResponse>> crear(
            @Validated(ClienteRequest.OnCreate.class) @Valid @RequestBody ClienteRequest request) {
        ClienteResponse response = ClienteResponse.from(clienteService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Cliente creado exitosamente", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClienteResponse>>> listar(
            @RequestParam(required = false) TipoCliente tipo,
            @RequestParam(required = false) EstadoCliente estado) {
        List<ClienteResponse> response = clienteService.list(tipo, estado).stream()
                .map(ClienteResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClienteResponse>> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(ClienteResponse.from(clienteService.findById(id))));
    }

    @GetMapping("/documento/{documento}")
    public ResponseEntity<ApiResponse<ClienteResponse>> buscarPorDocumento(@PathVariable String documento) {
        return ResponseEntity.ok(ApiResponse.ok(ClienteResponse.from(clienteService.findByDocumento(documento))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClienteResponse>> actualizar(
            @PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cliente actualizado correctamente",
                ClienteResponse.from(clienteService.update(id, request))));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<ClienteResponse>> cambiarEstado(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok("Estado del cliente actualizado correctamente",
                ClienteResponse.from(clienteService.changeEstado(id, body.get("estado")))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ClienteResponse>> eliminar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Cliente eliminado correctamente",
                ClienteResponse.from(clienteService.delete(id))));
    }
}