package com.servicioagua.lectura;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicioagua.common.enums.EstadoLectura;
import com.servicioagua.common.enums.EstadoMedidor;
import com.servicioagua.lectura.dto.LecturaRequest;
import com.servicioagua.medidor.Medidor;
import com.servicioagua.medidor.MedidorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class LecturaApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MedidorRepository medidorRepository;

    @Autowired
    private LecturaRepository lecturaRepository;

    private Medidor crearMedidor(String serie, Integer lecturaAnteriorAjuste) {
        return medidorRepository.save(Medidor.builder()
                .numeroSerie(serie)
                .lecturaAnteriorAjuste(lecturaAnteriorAjuste)
                .fechaInstalacion(LocalDate.of(2024, 1, 1))
                .estado(EstadoMedidor.ACTIVO)
                .build());
    }

    private void registrarLectura(long medidorId, int valor, LocalDate fecha) throws Exception {
        mockMvc.perform(post("/api/lecturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(LecturaRequest.builder()
                                .medidorId(medidorId)
                                .valorLectura(valor)
                                .fechaLectura(fecha)
                                .build())))
                .andExpect(status().isCreated());
    }

    private String lecturaJson(long medidorId, int valor, LocalDate fecha) throws Exception {
        return objectMapper.writeValueAsString(LecturaRequest.builder()
                .medidorId(medidorId)
                .valorLectura(valor)
                .fechaLectura(fecha)
                .build());
    }

    @Test
    void registrarSinLecturaPreviaDevuelve201ConConsumoIgualAlValor() throws Exception {
        Medidor medidor = crearMedidor("MTR-3001", null);

        mockMvc.perform(post("/api/lecturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lecturaJson(medidor.getId(), 150, LocalDate.of(2024, 5, 10))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.medidorId").value(medidor.getId()))
                .andExpect(jsonPath("$.data.valorLectura").value(150))
                .andExpect(jsonPath("$.data.consumoM3").value(150));
    }

    @Test
    void registrarUsandoLecturaAnteriorDeAjusteDevuelve201ConConsumoCalculado() throws Exception {
        Medidor medidor = crearMedidor("MTR-3002", 100);

        mockMvc.perform(post("/api/lecturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lecturaJson(medidor.getId(), 150, LocalDate.of(2024, 5, 10))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.consumoM3").value(50));
    }

    @Test
    void registrarConLecturaPreviaDevuelve201ConConsumoCalculado() throws Exception {
        Medidor medidor = crearMedidor("MTR-3003", null);
        lecturaRepository.save(Lectura.builder()
                .medidor(medidor)
                .valorLectura(100)
                .consumoM3(100)
                .fechaLectura(LocalDate.of(2024, 5, 1))
                .estado(EstadoLectura.ACTIVA)
                .build());

        mockMvc.perform(post("/api/lecturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lecturaJson(medidor.getId(), 150, LocalDate.of(2024, 5, 10))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.valorLectura").value(150))
                .andExpect(jsonPath("$.data.consumoM3").value(50));
    }

    @Test
    void registrarConConsumoNegativoDevuelve409() throws Exception {
        Medidor medidor = crearMedidor("MTR-3004", null);
        lecturaRepository.save(Lectura.builder()
                .medidor(medidor)
                .valorLectura(150)
                .consumoM3(150)
                .fechaLectura(LocalDate.of(2024, 5, 1))
                .estado(EstadoLectura.ACTIVA)
                .build());

        mockMvc.perform(post("/api/lecturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lecturaJson(medidor.getId(), 100, LocalDate.of(2024, 5, 10))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void registrarConMedidorInexistenteDevuelve404() throws Exception {
        mockMvc.perform(post("/api/lecturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lecturaJson(999999L, 100, LocalDate.of(2024, 5, 10))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void registrarDobleLecturaMismoMedidorYFechaDevuelve409() throws Exception {
        Medidor medidor = crearMedidor("MTR-3005", null);
        registrarLectura(medidor.getId(), 100, LocalDate.of(2024, 5, 10));

        mockMvc.perform(post("/api/lecturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lecturaJson(medidor.getId(), 150, LocalDate.of(2024, 5, 10))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void anularYReRegistrarEnLaMismaFechaDevuelve201() throws Exception {
        Medidor medidor = crearMedidor("MTR-3007", null);
        Lectura lectura = lecturaRepository.save(Lectura.builder()
                .medidor(medidor)
                .valorLectura(100)
                .consumoM3(100)
                .fechaLectura(LocalDate.of(2024, 5, 10))
                .estado(EstadoLectura.ACTIVA)
                .build());

        mockMvc.perform(put("/api/lecturas/{id}/anular", lectura.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/lecturas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lecturaJson(medidor.getId(), 150, LocalDate.of(2024, 5, 10))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.valorLectura").value(150));
    }

    @Test
    void historialMedidorDevuelve200() throws Exception {
        Medidor medidor = crearMedidor("MTR-3006", null);
        registrarLectura(medidor.getId(), 100, LocalDate.of(2024, 5, 1));
        registrarLectura(medidor.getId(), 150, LocalDate.of(2024, 5, 10));

        mockMvc.perform(get("/api/lecturas/medidor/{id}", medidor.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].valorLectura").value(150))
                .andExpect(jsonPath("$.data[0].consumoM3").value(50));
    }
}