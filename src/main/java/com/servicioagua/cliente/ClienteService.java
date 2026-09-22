package com.servicioagua.cliente;

import com.servicioagua.cliente.dto.ClienteRequest;
import com.servicioagua.common.enums.EstadoCliente;
import com.servicioagua.common.enums.TipoCliente;
import com.servicioagua.common.exception.DuplicateResourceException;
import com.servicioagua.common.exception.ResourceNotFoundException;
import com.servicioagua.factura.FacturaRepository;
import com.servicioagua.toma.TomaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final TomaRepository tomaRepository;
    private final FacturaRepository facturaRepository;

    @Transactional
    public Cliente create(ClienteRequest request) {
        if (clienteRepository.existsByDocumento(request.getDocumento())) {
            throw new DuplicateResourceException("Ya existe un cliente con el documento " + request.getDocumento());
        }
        Cliente cliente = Cliente.builder()
                .documento(request.getDocumento())
                .nombre(request.getNombre())
                .telefono(request.getTelefono())
                .email(request.getEmail())
                .direccion(request.getDireccion())
                .tipo(request.getTipo())
                .estado(EstadoCliente.ACTIVO)
                .fechaRegistro(LocalDateTime.now())
                .build();
        return clienteRepository.save(cliente);
    }

    @Transactional(readOnly = true)
    public List<Cliente> list(TipoCliente tipo, EstadoCliente estado) {
        return clienteRepository.findAll().stream()
                .filter(c -> tipo == null || c.getTipo() == tipo)
                .filter(c -> estado == null || c.getEstado() == estado)
                .toList();
    }

    @Transactional(readOnly = true)
    public Cliente findById(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("cliente", id));
    }

    @Transactional(readOnly = true)
    public Cliente findByDocumento(String documento) {
        return clienteRepository.findByDocumento(documento)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró un cliente con documento " + documento));
    }

    @Transactional
    public Cliente update(Long id, ClienteRequest request) {
        Cliente cliente = findById(id);
        if (request.getDocumento() != null) {
            if (request.getDocumento().isBlank()) {
                throw new IllegalArgumentException("El documento no puede estar vacío");
            }
            if (!request.getDocumento().equals(cliente.getDocumento())
                    && clienteRepository.existsByDocumento(request.getDocumento())) {
                throw new DuplicateResourceException("Ya existe un cliente con el documento " + request.getDocumento());
            }
            cliente.setDocumento(request.getDocumento());
        }
        if (request.getNombre() != null) {
            if (request.getNombre().isBlank()) {
                throw new IllegalArgumentException("El nombre no puede estar vacío");
            }
            cliente.setNombre(request.getNombre());
        }
        if (request.getTelefono() != null) {
            cliente.setTelefono(request.getTelefono());
        }
        if (request.getEmail() != null) {
            cliente.setEmail(request.getEmail());
        }
        if (request.getDireccion() != null) {
            cliente.setDireccion(request.getDireccion());
        }
        if (request.getTipo() != null) {
            cliente.setTipo(request.getTipo());
        }
        return clienteRepository.save(cliente);
    }

    @Transactional
    public Cliente changeEstado(Long id, String estado) {
        Cliente cliente = findById(id);
        EstadoCliente nuevoEstado;
        try {
            nuevoEstado = EstadoCliente.valueOf(estado.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalArgumentException("Estado de cliente no válido: " + estado);
        }
        cliente.setEstado(nuevoEstado);
        return clienteRepository.save(cliente);
    }

    @Transactional
    public Cliente delete(Long id) {
        Cliente cliente = findById(id);
        boolean tieneTomas = !tomaRepository.findByClienteId(id).isEmpty();
        boolean tieneFacturas = !facturaRepository.findByClienteIdOrderByPeriodoDesdeDesc(id).isEmpty();
        if (tieneTomas || tieneFacturas) {
            cliente.setEstado(EstadoCliente.INACTIVO);
            return clienteRepository.save(cliente);
        }
        clienteRepository.delete(cliente);
        return cliente;
    }
}