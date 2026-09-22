package com.servicioagua.cliente.dto;

import com.servicioagua.cliente.Cliente;
import com.servicioagua.common.enums.EstadoCliente;
import com.servicioagua.common.enums.TipoCliente;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteResponse {

    private Long id;
    private String documento;
    private String nombre;
    private String telefono;
    private String email;
    private String direccion;
    private TipoCliente tipo;
    private EstadoCliente estado;
    private LocalDateTime fechaRegistro;

    public static ClienteResponse from(Cliente cliente) {
        return ClienteResponse.builder()
                .id(cliente.getId())
                .documento(cliente.getDocumento())
                .nombre(cliente.getNombre())
                .telefono(cliente.getTelefono())
                .email(cliente.getEmail())
                .direccion(cliente.getDireccion())
                .tipo(cliente.getTipo())
                .estado(cliente.getEstado())
                .fechaRegistro(cliente.getFechaRegistro())
                .build();
    }
}