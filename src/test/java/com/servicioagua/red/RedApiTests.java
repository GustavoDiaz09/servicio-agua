package com.servicioagua.red;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicioagua.common.enums.EstadoFuga;
import com.servicioagua.common.enums.EstadoMantenimiento;
import com.servicioagua.common.enums.SeveridadFuga;
import com.servicioagua.common.enums.TipoMantenimiento;
import com.servicioagua.red.dto.FugaRequest;
import com.servicioagua.red.dto.MantenimientoEstadoRequest;
import com.servicioagua.red.dto.MantenimientoRequest;
import com.servicioagua.red.dto.SectorRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
@Transactional
class RedApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private TuberiaRepository tuberiaRepository;

    @Autowired
    private FugaRepository fugaRepository;

    @Autowired
    private MantenimientoRepository mantenimientoRepository;

    private int secuencia;

    private Sector crearSector(String nombre) {
        return sectorRepository.save(Sector.builder()
                .nombre(nombre)
                .zona("Zona X")
                .presionPromedio(new BigDecimal("3.10"))
                .build());
    }

    private String sectorJson(String nombre) throws Exception {
        return objectMapper.writeValueAsString(SectorRequest.builder()
                .nombre(nombre)
                .zona("Zona X")
                .presionPromedio(new BigDecimal("3.10"))
                .build());
    }

    private long crearFugaPorApi(Sector sector) throws Exception {
        String body = mockMvc.perform(post("/api/fugas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(FugaRequest.builder()
                                .sectorId(sector.getId())
                                .ubicacion("Calle Fuga " + ++secuencia)
                                .severidad(SeveridadFuga.ALTA)
                                .build())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asLong();
    }

    private Sector crearSector() {
        return crearSector("Sector Red " + ++secuencia);
    }

    @Test
    void crearSectorDevuelve201() throws Exception {
        mockMvc.perform(post("/api/sectores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sectorJson("Sector Nuevo")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.nombre").value("Sector Nuevo"))
                .andExpect(jsonPath("$.data.zona").value("Zona X"));
    }

    @Test
    void crearSectorConNombreDuplicadoDevuelve409() throws Exception {
        crearSector("Sector Duplicado");

        mockMvc.perform(post("/api/sectores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sectorJson("Sector Duplicado")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void obtenerSectorPorIdDevuelve200() throws Exception {
        Sector sector = crearSector();

        mockMvc.perform(get("/api/sectores/{id}", sector.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(sector.getId()))
                .andExpect(jsonPath("$.data.nombre").value(sector.getNombre()));
    }

    @Test
    void obtenerSectorInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/sectores/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void eliminarSectorConTuberiaAsociadaDevuelve409() throws Exception {
        Sector sector = crearSector();
        tuberiaRepository.save(Tuberia.builder()
                .sector(sector)
                .material("PVC")
                .diametroMm(new BigDecimal("100.00"))
                .longitudM(new BigDecimal("250.00"))
                .tramo("T-1")
                .fechaInstalacion(LocalDate.of(2023, 1, 1))
                .build());

        mockMvc.perform(delete("/api/sectores/{id}", sector.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void reportarFugaDevuelve201ConEstadoReportada() throws Exception {
        Sector sector = crearSector();

        mockMvc.perform(post("/api/fugas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(FugaRequest.builder()
                                .sectorId(sector.getId())
                                .ubicacion("Esquina Av. Central")
                                .severidad(SeveridadFuga.CRITICA)
                                .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.sectorId").value(sector.getId()))
                .andExpect(jsonPath("$.data.estado").value("REPORTADA"));
    }

    @Test
    void pasarFugaEnCursoDevuelve200() throws Exception {
        Sector sector = crearSector();
        long fugaId = crearFugaPorApi(sector);

        mockMvc.perform(put("/api/fugas/{id}/en-curso", fugaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(fugaId))
                .andExpect(jsonPath("$.data.estado").value("EN_CURSO"));
    }

    @Test
    void resolverFugaDevuelve200ConFechaResolucion() throws Exception {
        Sector sector = crearSector();
        long fugaId = crearFugaPorApi(sector);

        mockMvc.perform(put("/api/fugas/{id}/resolver", fugaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.estado").value("RESUELTA"))
                .andExpect(jsonPath("$.data.fechaResolucion").exists());
    }

    @Test
    void resolverFugaYaResueltaDevuelve409() throws Exception {
        Sector sector = crearSector();
        Fuga fuga = fugaRepository.save(Fuga.builder()
                .sector(sector)
                .ubicacion("Calle Resuelta")
                .severidad(SeveridadFuga.MEDIA)
                .estado(EstadoFuga.RESUELTA)
                .fechaReporte(LocalDate.now())
                .fechaResolucion(LocalDate.now())
                .build());

        mockMvc.perform(put("/api/fugas/{id}/resolver", fuga.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void crearMantenimientoConFechaPasadaDevuelve400() throws Exception {
        Sector sector = crearSector();

        mockMvc.perform(post("/api/mantenimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(MantenimientoRequest.builder()
                                .sectorId(sector.getId())
                                .descripcion("Mantenimiento pasado")
                                .tipo(TipoMantenimiento.PREVENTIVO)
                                .fechaProgramada(LocalDate.of(2000, 1, 1))
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void transicionMantenimientoInvalidaDevuelve409() throws Exception {
        Sector sector = crearSector();
        Mantenimiento mantenimiento = mantenimientoRepository.save(Mantenimiento.builder()
                .sector(sector)
                .descripcion("Mantenimiento programado")
                .tipo(TipoMantenimiento.CORRECTIVO)
                .fechaProgramada(LocalDate.of(2030, 1, 1))
                .estado(EstadoMantenimiento.PROGRAMADO)
                .build());

        mockMvc.perform(put("/api/mantenimientos/{id}/estado", mantenimiento.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(MantenimientoEstadoRequest.builder()
                                .estado(EstadoMantenimiento.COMPLETADO)
                                .build())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void actualizarFugaDevuelve200() throws Exception {
        Sector sector = crearSector();
        long fugaId = crearFugaPorApi(sector);

        mockMvc.perform(put("/api/fugas/{id}", fugaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(FugaRequest.builder()
                                .sectorId(sector.getId())
                                .ubicacion("Nueva ubicación fuga")
                                .severidad(SeveridadFuga.MEDIA)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(fugaId))
                .andExpect(jsonPath("$.data.ubicacion").value("Nueva ubicación fuga"))
                .andExpect(jsonPath("$.data.severidad").value("MEDIA"))
                .andExpect(jsonPath("$.data.estado").value("REPORTADA"));
    }

    @Test
    void actualizarFugaResueltaDevuelve409() throws Exception {
        Sector sector = crearSector();
        Fuga fuga = fugaRepository.save(Fuga.builder()
                .sector(sector)
                .ubicacion("Calle Resuelta")
                .severidad(SeveridadFuga.MEDIA)
                .estado(EstadoFuga.RESUELTA)
                .fechaReporte(LocalDate.now())
                .fechaResolucion(LocalDate.now())
                .build());

        mockMvc.perform(put("/api/fugas/{id}", fuga.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(FugaRequest.builder()
                                .sectorId(sector.getId())
                                .ubicacion("Intento de edición")
                                .severidad(SeveridadFuga.ALTA)
                                .build())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void actualizarFugaInexistenteDevuelve404() throws Exception {
        Sector sector = crearSector();

        mockMvc.perform(put("/api/fugas/{id}", 999999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(FugaRequest.builder()
                                .sectorId(sector.getId())
                                .ubicacion("Sin fuga")
                                .severidad(SeveridadFuga.ALTA)
                                .build())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void eliminarFugaDevuelve200YDespues404() throws Exception {
        Sector sector = crearSector();
        long fugaId = crearFugaPorApi(sector);

        mockMvc.perform(delete("/api/fugas/{id}", fugaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/fugas/{id}", fugaId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void actualizarMantenimientoDevuelve200() throws Exception {
        Sector sector = crearSector();
        String mantId = mockMvc.perform(post("/api/mantenimientos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(MantenimientoRequest.builder()
                                .sectorId(sector.getId())
                                .descripcion("Mantenimiento inicial")
                                .tipo(TipoMantenimiento.PREVENTIVO)
                                .fechaProgramada(LocalDate.of(2030, 1, 1))
                                .build())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(mantId).path("data").path("id").asLong();

        mockMvc.perform(put("/api/mantenimientos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(MantenimientoRequest.builder()
                                .sectorId(sector.getId())
                                .descripcion("Mantenimiento actualizado")
                                .tipo(TipoMantenimiento.CORRECTIVO)
                                .fechaProgramada(LocalDate.of(2030, 2, 1))
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.descripcion").value("Mantenimiento actualizado"))
                .andExpect(jsonPath("$.data.tipo").value("CORRECTIVO"))
                .andExpect(jsonPath("$.data.estado").value("PROGRAMADO"));
    }

    @Test
    void actualizarMantenimientoNoProgramadoDevuelve409() throws Exception {
        Sector sector = crearSector();
        Mantenimiento mantenimiento = mantenimientoRepository.save(Mantenimiento.builder()
                .sector(sector)
                .descripcion("Mantenimiento en curso")
                .tipo(TipoMantenimiento.CORRECTIVO)
                .fechaProgramada(LocalDate.of(2030, 1, 1))
                .estado(EstadoMantenimiento.EN_CURSO)
                .build());

        mockMvc.perform(put("/api/mantenimientos/{id}", mantenimiento.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(MantenimientoRequest.builder()
                                .sectorId(sector.getId())
                                .descripcion("Intento de edición")
                                .tipo(TipoMantenimiento.PREVENTIVO)
                                .fechaProgramada(LocalDate.of(2030, 2, 1))
                                .build())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void eliminarMantenimientoDevuelve200YDespues404() throws Exception {
        Sector sector = crearSector();
        Mantenimiento mantenimiento = mantenimientoRepository.save(Mantenimiento.builder()
                .sector(sector)
                .descripcion("Mantenimiento a eliminar")
                .tipo(TipoMantenimiento.PREVENTIVO)
                .fechaProgramada(LocalDate.of(2030, 1, 1))
                .estado(EstadoMantenimiento.PROGRAMADO)
                .build());

        mockMvc.perform(delete("/api/mantenimientos/{id}", mantenimiento.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/mantenimientos/{id}", mantenimiento.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}