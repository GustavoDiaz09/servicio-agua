package com.servicioagua.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
@Transactional
class CommonApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rutaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/ruta-inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("No se encontró el recurso solicitado"));
    }

    @Test
    void rutaInexistentePostDevuelve404() throws Exception {
        mockMvc.perform(post("/api/ruta-inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}