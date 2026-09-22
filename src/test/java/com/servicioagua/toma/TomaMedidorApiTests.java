package com.servicioagua.toma;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicioagua.cliente.Cliente;
import com.servicioagua.cliente.ClienteRepository;
import com.servicioagua.common.enums.EstadoCliente;
import com.servicioagua.common.enums.EstadoLectura;
import com.servicioagua.common.enums.TipoCliente;
import com.servicioagua.lectura.Lectura;
import com.servicioagua.lectura.LecturaRepository;
import com.servicioagua.medidor.Medidor;
import com.servicioagua.medidor.MedidorRepository;
import com.servicioagua.medidor.dto.MedidorAsignacionRequest;
import com.servicioagua.medidor.dto.MedidorRequest;
import com.servicioagua.red.Sector;
import com.servicioagua.red.SectorRepository;
import com.servicioagua.toma.dto.TomaRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
@Transactional
class TomaMedidorApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private MedidorRepository medidorRepository;

    @Autowired
    private LecturaRepository lecturaRepository;

    private Sector crearSector() {
        return sectorRepository.save(Sector.builder()
                .nombre("Sector Norte Test")
                .zona("Zona A")
                .presionPromedio(new BigDecimal("3.40"))
                .build());
    }

    private Cliente crearCliente() {
        return clienteRepository.save(Cliente.builder()
                .documento("1122334455")
                .nombre("María Pérez")
                .telefono("3001234567")
                .email("maria@correo.com")
                .direccion("Calle 10 #20-30")
                .tipo(TipoCliente.RESIDENCIAL)
                .estado(EstadoCliente.ACTIVO)
                .build());
    }

    private long crearTomaPorApi(String codigo, long sectorId, long clienteId) throws Exception {
        String body = mockMvc.perform(post("/api/tomas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                TomaRequest.builder()
                                        .codigo(codigo)
                                        .sectorId(sectorId)
                                        .clienteId(clienteId)
                                        .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asLong();
    }

    private long crearMedidorPorApi(String serie) throws Exception {
        String body = mockMvc.perform(post("/api/medidores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                MedidorRequest.builder().numeroSerie(serie).build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asLong();
    }

    private void asignarMedidor(long medidorId, long tomaId) throws Exception {
        mockMvc.perform(put("/api/medidores/{id}/asignar", medidorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                MedidorAsignacionRequest.builder().tomaId(tomaId).build())))
                .andExpect(status().isOk());
    }

    @Test
    void crearTomaDevuelve201() throws Exception {
        Sector sector = crearSector();
        Cliente cliente = crearCliente();
        mockMvc.perform(post("/api/tomas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                TomaRequest.builder()
                                        .codigo("TOM-0001")
                                        .sectorId(sector.getId())
                                        .clienteId(cliente.getId())
                                        .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.codigo").value("TOM-0001"))
                .andExpect(jsonPath("$.data.sectorId").value(sector.getId()))
                .andExpect(jsonPath("$.data.sectorNombre").value("Sector Norte Test"))
                .andExpect(jsonPath("$.data.clienteId").value(cliente.getId()))
                .andExpect(jsonPath("$.data.estado").value("ACTIVA"));
    }

    @Test
    void crearTomaConCodigoDuplicadoDevuelve409() throws Exception {
        Sector sector = crearSector();
        Cliente cliente = crearCliente();
        crearTomaPorApi("TOM-0002", sector.getId(), cliente.getId());

        mockMvc.perform(post("/api/tomas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                TomaRequest.builder()
                                        .codigo("TOM-0002")
                                        .sectorId(sector.getId())
                                        .clienteId(cliente.getId())
                                        .build())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void crearMedidorDevuelve201() throws Exception {
        mockMvc.perform(post("/api/medidores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                MedidorRequest.builder().numeroSerie("MTR-2001").build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.numeroSerie").value("MTR-2001"))
                .andExpect(jsonPath("$.data.estado").value("ACTIVO"));
    }

    @Test
    void crearMedidorConSerieDuplicadaDevuelve409() throws Exception {
        crearMedidorPorApi("MTR-2002");
        mockMvc.perform(post("/api/medidores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                MedidorRequest.builder().numeroSerie("MTR-2002").build())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void asignarMedidorATomaDevuelve200() throws Exception {
        Sector sector = crearSector();
        Cliente cliente = crearCliente();
        long tomaId = crearTomaPorApi("TOM-0005", sector.getId(), cliente.getId());
        long medidorId = crearMedidorPorApi("MTR-2005");

        mockMvc.perform(put("/api/medidores/{id}/asignar", medidorId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                MedidorAsignacionRequest.builder().tomaId(tomaId).build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tomaId").value(tomaId))
                .andExpect(jsonPath("$.data.estado").value("ACTIVO"));
    }

    @Test
    void asignarSegundoMedidorActivoALaMismaTomaDevuelve409() throws Exception {
        Sector sector = crearSector();
        Cliente cliente = crearCliente();
        long tomaId = crearTomaPorApi("TOM-0006", sector.getId(), cliente.getId());
        long medidor1 = crearMedidorPorApi("MTR-2006");
        long medidor2 = crearMedidorPorApi("MTR-2007");

        asignarMedidor(medidor1, tomaId);

        mockMvc.perform(put("/api/medidores/{id}/asignar", medidor2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                MedidorAsignacionRequest.builder().tomaId(tomaId).build())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void darDeBajaConLecturasPendientesDevuelve409() throws Exception {
        Sector sector = crearSector();
        Cliente cliente = crearCliente();
        long tomaId = crearTomaPorApi("TOM-0010", sector.getId(), cliente.getId());
        long medidorId = crearMedidorPorApi("MTR-2010");
        asignarMedidor(medidorId, tomaId);
        Medidor medidor = medidorRepository.findById(medidorId).orElseThrow();
        lecturaRepository.save(Lectura.builder()
                .medidor(medidor)
                .valorLectura(100)
                .consumoM3(100)
                .fechaLectura(LocalDate.now())
                .estado(EstadoLectura.ACTIVA)
                .build());

        mockMvc.perform(put("/api/tomas/{id}/baja", tomaId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void darDeBajaSinLecturasPendientesDevuelve200() throws Exception {
        Sector sector = crearSector();
        Cliente cliente = crearCliente();
        long tomaId = crearTomaPorApi("TOM-0011", sector.getId(), cliente.getId());

        mockMvc.perform(put("/api/tomas/{id}/baja", tomaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.estado").value("DADA_DE_BAJA"));
    }

    @Test
    void retirarMedidorDevuelve200() throws Exception {
        Sector sector = crearSector();
        Cliente cliente = crearCliente();
        long tomaId = crearTomaPorApi("TOM-0007", sector.getId(), cliente.getId());
        long medidorId = crearMedidorPorApi("MTR-2008");

        asignarMedidor(medidorId, tomaId);

        mockMvc.perform(put("/api/medidores/{id}/retirar", medidorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.estado").value("RETIRADO"))
                .andExpect(jsonPath("$.data.tomaId").value(nullValue()));
    }
}