package com.servicioagua;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ServicioAguaApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertNotNull(context);
    }

    @Test
    void controladoresRegistrados() {
        assertTrue(context.containsBean("clienteController"));
        assertTrue(context.containsBean("tomaController"));
        assertTrue(context.containsBean("medidorController"));
        assertTrue(context.containsBean("tarifaController"));
        assertTrue(context.containsBean("lecturaController"));
        assertTrue(context.containsBean("facturaController"));
        assertTrue(context.containsBean("pagoController"));
        assertTrue(context.containsBean("sectorController"));
        assertTrue(context.containsBean("tuberiaController"));
        assertTrue(context.containsBean("fugaController"));
        assertTrue(context.containsBean("mantenimientoController"));
        assertTrue(context.containsBean("reporteController"));
    }
}