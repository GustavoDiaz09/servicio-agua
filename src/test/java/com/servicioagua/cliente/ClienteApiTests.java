package com.servicioagua.cliente;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicioagua.cliente.dto.ClienteRequest;
import com.servicioagua.common.enums.EstadoCliente;
import com.servicioagua.common.enums.TipoCliente;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
@Transactional
class ClienteApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClienteRepository clienteRepository;

    private ClienteRequest clienteRequest(String documento, String nombre) {
        return ClienteRequest.builder()
                .documento(documento)
                .nombre(nombre)
                .telefono("3001234567")
                .email("cliente@correo.com")
                .direccion("Calle 1 #2-3")
                .tipo(TipoCliente.RESIDENCIAL)
                .build();
    }

    private Cliente crearCliente(String documento, String nombre) {
        return clienteRepository.save(Cliente.builder()
                .documento(documento)
                .nombre(nombre)
                .telefono("3001234567")
                .email("cliente@correo.com")
                .direccion("Calle 1 #2-3")
                .tipo(TipoCliente.RESIDENCIAL)
                .estado(EstadoCliente.ACTIVO)
                .build());
    }

    private long extraerId(String body) throws Exception {
        JsonNode node = objectMapper.readTree(body);
        return node.path("data").path("id").asLong();
    }

    @Test
    void crearClienteDevuelve201ConIdAsignado() throws Exception {
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clienteRequest("1234567890", "Juan Pérez"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.documento").value("1234567890"))
                .andExpect(jsonPath("$.data.nombre").value("Juan Pérez"))
                .andExpect(jsonPath("$.data.estado").value("ACTIVO"));
    }

    @Test
    void crearClienteConDocumentoDuplicadoDevuelve409() throws Exception {
        crearCliente("1234567890", "Ana López");
        mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clienteRequest("1234567890", "Otra Persona"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listarClientesDevuelve200() throws Exception {
        crearCliente("1234567890", "Juan Pérez");
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[0].documento").exists());
    }

    @Test
    void buscarClientePorIdDevuelve200() throws Exception {
        Cliente cliente = crearCliente("1234567890", "Juan Pérez");
        mockMvc.perform(get("/api/clientes/{id}", cliente.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(cliente.getId()))
                .andExpect(jsonPath("$.data.nombre").value("Juan Pérez"));
    }

    @Test
    void buscarClienteInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/clientes/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void actualizarClienteDevuelve200() throws Exception {
        String crear = mockMvc.perform(post("/api/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clienteRequest("1234567890", "Juan Pérez"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = extraerId(crear);

        mockMvc.perform(put("/api/clientes/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clienteRequest("1234567890", "Juan Pérez Actualizado"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.nombre").value("Juan Pérez Actualizado"));
    }

    @Test
    void actualizarClienteConDocumentoVacioDevuelve400() throws Exception {
        Cliente cliente = crearCliente("1234567890", "Juan Pérez");

        mockMvc.perform(put("/api/clientes/{id}", cliente.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ClienteRequest.builder()
                                .documento("")
                                .nombre("Juan Pérez")
                                .telefono("3001234567")
                                .email("cliente@correo.com")
                                .direccion("Calle 1 #2-3")
                                .tipo(TipoCliente.RESIDENCIAL)
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void cambiarEstadoClienteDevuelve200() throws Exception {
        Cliente cliente = crearCliente("1234567890", "Juan Pérez");
        mockMvc.perform(put("/api/clientes/{id}/estado", cliente.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"SUSPENDIDO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("SUSPENDIDO"));
    }

    @Test
    void eliminarClienteSinActividadDevuelve200YRealizaBorradoFisico() throws Exception {
        Cliente cliente = crearCliente("1234567890", "Juan Pérez");
        mockMvc.perform(delete("/api/clientes/{id}", cliente.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(cliente.getId()));

        assertFalse(clienteRepository.existsById(cliente.getId()));
        mockMvc.perform(get("/api/clientes/{id}", cliente.getId()))
                .andExpect(status().isNotFound());
    }
}