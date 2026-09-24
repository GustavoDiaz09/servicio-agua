package com.servicioagua.tarifa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicioagua.tarifa.dto.TarifaRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class TarifaApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TarifaRepository tarifaRepository;

    private String tarifaJson(String nombre, int desde, int hasta, String precio, String cargoFijo,
                              LocalDate vigenteDesde, LocalDate vigenteHasta) throws Exception {
        return objectMapper.writeValueAsString(TarifaRequest.builder()
                .nombre(nombre)
                .desdeM3(desde)
                .hastaM3(hasta)
                .precioPorM3(new BigDecimal(precio))
                .cargoFijo(new BigDecimal(cargoFijo))
                .vigenteDesde(vigenteDesde)
                .vigenteHasta(vigenteHasta)
                .build());
    }

    @Test
    void crearTramoDevuelve201() throws Exception {
        mockMvc.perform(post("/api/tarifas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tarifaJson("Tarifa base", 0, 10, "1.5000", "12500.00",
                                LocalDate.of(2024, 1, 1), null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.nombre").value("Tarifa base"))
                .andExpect(jsonPath("$.data.desdeM3").value(0))
                .andExpect(jsonPath("$.data.hastaM3").value(10));
    }

    @Test
    void crearTramoConRangoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/tarifas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tarifaJson("Tarifa inválida", 10, 10, "1.5000", "0.00",
                                LocalDate.of(2024, 1, 1), null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void crearTramoSolapadoDevuelve409() throws Exception {
        mockMvc.perform(post("/api/tarifas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tarifaJson("Tarifa base", 0, 10, "1.5000", "12500.00",
                                LocalDate.of(2024, 1, 1), null)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/tarifas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tarifaJson("Tarifa solapada", 5, 20, "2.0000", "0.00",
                                LocalDate.of(2024, 6, 1), null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listarVigentesDevuelve200() throws Exception {
        mockMvc.perform(post("/api/tarifas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tarifaJson("Tarifa vigente", 0, 10, "1.5000", "12500.00",
                                LocalDate.now().minusYears(1), null)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tarifas/vigentes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[0].nombre").exists());
    }

    @Test
    void eliminarTarifaVigenteDevuelve409() throws Exception {
        Tarifa tarifa = tarifaRepository.save(Tarifa.builder()
                .nombre("Tarifa Vigente Eliminar")
                .desdeM3(0)
                .hastaM3(10)
                .precioPorM3(new BigDecimal("1.5000"))
                .vigenteDesde(LocalDate.of(2024, 1, 1))
                .build());

        mockMvc.perform(delete("/api/tarifas/{id}", tarifa.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void eliminarTarifaExpiradaDevuelve200() throws Exception {
        Tarifa tarifa = tarifaRepository.save(Tarifa.builder()
                .nombre("Tarifa Expirada Eliminar")
                .desdeM3(0)
                .hastaM3(10)
                .precioPorM3(new BigDecimal("1.5000"))
                .vigenteDesde(LocalDate.of(2019, 1, 1))
                .vigenteHasta(LocalDate.of(2019, 12, 31))
                .build());

        mockMvc.perform(delete("/api/tarifas/{id}", tarifa.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void listarVigentesConFechaParametrizadaDevuelve200() throws Exception {
        mockMvc.perform(post("/api/tarifas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tarifaJson("Tarifa 2024", 0, 30, "2.0000", "0.00",
                                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tarifas/vigentes").param("fecha", "2024-06-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[0].nombre").value("Tarifa 2024"));

        mockMvc.perform(get("/api/tarifas/vigentes").param("fecha", "2025-06-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}