package com.servicioagua.factura;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicioagua.cliente.Cliente;
import com.servicioagua.cliente.ClienteRepository;
import com.servicioagua.common.enums.EstadoCliente;
import com.servicioagua.common.enums.EstadoFactura;
import com.servicioagua.common.enums.EstadoLectura;
import com.servicioagua.common.enums.EstadoMedidor;
import com.servicioagua.common.enums.EstadoToma;
import com.servicioagua.common.enums.TipoCliente;
import com.servicioagua.factura.dto.GenerarFacturaRequest;
import com.servicioagua.lectura.Lectura;
import com.servicioagua.lectura.LecturaRepository;
import com.servicioagua.medidor.Medidor;
import com.servicioagua.medidor.MedidorRepository;
import com.servicioagua.red.Sector;
import com.servicioagua.red.SectorRepository;
import com.servicioagua.tarifa.Tarifa;
import com.servicioagua.tarifa.TarifaRepository;
import com.servicioagua.toma.Toma;
import com.servicioagua.toma.TomaRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class FacturaApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private TomaRepository tomaRepository;

    @Autowired
    private MedidorRepository medidorRepository;

    @Autowired
    private LecturaRepository lecturaRepository;

    @Autowired
    private TarifaRepository tarifaRepository;

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private FacturaVencimientoScheduler facturaVencimientoScheduler;

    private int secuencia;

    private Cliente crearCliente(String nombre) {
        return clienteRepository.save(Cliente.builder()
                .documento("DOC-FAC-" + ++secuencia)
                .nombre(nombre)
                .telefono("3000000000")
                .email("cliente@correo.com")
                .direccion("Calle 1 #2-3")
                .tipo(TipoCliente.RESIDENCIAL)
                .estado(EstadoCliente.ACTIVO)
                .build());
    }

    private Sector crearSector() {
        return sectorRepository.save(Sector.builder()
                .nombre("Sector Factura " + ++secuencia)
                .zona("Zona A")
                .presionPromedio(new BigDecimal("3.40"))
                .build());
    }

    private Medidor crearMedidorConToma(Cliente cliente, Sector sector) {
        Toma toma = tomaRepository.save(Toma.builder()
                .codigo("TOM-FAC-" + ++secuencia)
                .sector(sector)
                .cliente(cliente)
                .fechaAlta(LocalDate.of(2023, 1, 1))
                .estado(EstadoToma.ACTIVA)
                .build());
        return medidorRepository.save(Medidor.builder()
                .numeroSerie("MTR-FAC-" + secuencia)
                .toma(toma)
                .fechaInstalacion(LocalDate.of(2023, 1, 1))
                .estado(EstadoMedidor.ACTIVO)
                .build());
    }

    private void crearLectura(Medidor medidor, LocalDate fecha, int valor, int consumo) {
        lecturaRepository.save(Lectura.builder()
                .medidor(medidor)
                .valorLectura(valor)
                .consumoM3(consumo)
                .fechaLectura(fecha)
                .estado(EstadoLectura.ACTIVA)
                .build());
    }

    private void crearTarifasVigentes() {
        LocalDate vigenteDesde = LocalDate.of(2024, 1, 1);
        tarifaRepository.save(Tarifa.builder()
                .nombre("Tarifa Base")
                .desdeM3(0)
                .hastaM3(10)
                .precioPorM3(new BigDecimal("1.5000"))
                .cargoFijo(new BigDecimal("12500.00"))
                .vigenteDesde(vigenteDesde)
                .build());
        tarifaRepository.save(Tarifa.builder()
                .nombre("Tarifa Tramo 2")
                .desdeM3(11)
                .hastaM3(30)
                .precioPorM3(new BigDecimal("2.2500"))
                .vigenteDesde(vigenteDesde)
                .build());
        tarifaRepository.save(Tarifa.builder()
                .nombre("Tarifa Tramo 3")
                .desdeM3(31)
                .hastaM3(99999)
                .precioPorM3(new BigDecimal("3.0000"))
                .vigenteDesde(vigenteDesde)
                .build());
    }

    private Factura crearFactura(Cliente cliente, EstadoFactura estado, BigDecimal total, LocalDate vencimiento) {
        return facturaRepository.save(Factura.builder()
                .numero("FAC-TEST-" + ++secuencia)
                .cliente(cliente)
                .consumoM3(30)
                .subtotal(total)
                .cargoFijo(BigDecimal.ZERO)
                .total(total)
                .periodoDesde(LocalDate.of(2024, 5, 1))
                .periodoHasta(LocalDate.of(2024, 5, 31))
                .vencimiento(vencimiento)
                .estado(estado)
                .build());
    }

    private String generarFacturaJson(Long clienteId) throws Exception {
        return objectMapper.writeValueAsString(GenerarFacturaRequest.builder()
                .clienteId(clienteId)
                .periodoDesde(LocalDate.of(2024, 5, 1))
                .periodoHasta(LocalDate.of(2024, 5, 31))
                .vencimiento(LocalDate.of(2024, 6, 10))
                .build());
    }

    @Test
    void generarFacturaConLecturasDevuelve201ConTotalesCorrectos() throws Exception {
        Cliente cliente = crearCliente("Ana Facturada");
        Sector sector = crearSector();
        Medidor medidor = crearMedidorConToma(cliente, sector);
        crearLectura(medidor, LocalDate.of(2024, 5, 10), 130, 30);
        crearTarifasVigentes();

        mockMvc.perform(post("/api/facturas/generar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generarFacturaJson(cliente.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.clienteId").value(cliente.getId()))
                .andExpect(jsonPath("$.data.consumoM3").value(30))
                .andExpect(jsonPath("$.data.subtotal").value(60.00))
                .andExpect(jsonPath("$.data.cargoFijo").value(12500.00))
                .andExpect(jsonPath("$.data.total").value(12560.00))
                .andExpect(jsonPath("$.data.periodoDesde").value("2024-05-01"))
                .andExpect(jsonPath("$.data.estado").value("PENDIENTE"));
    }

    @Test
    void generarFacturaDuplicadaEnPeriodoDevuelve409() throws Exception {
        Cliente cliente = crearCliente("Cliente Duplicado");
        Sector sector = crearSector();
        Medidor medidor = crearMedidorConToma(cliente, sector);
        crearLectura(medidor, LocalDate.of(2024, 5, 10), 130, 30);
        crearTarifasVigentes();
        String body = generarFacturaJson(cliente.getId());

        mockMvc.perform(post("/api/facturas/generar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/facturas/generar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void generarFacturaSinLecturasEnPeriodoDevuelve409() throws Exception {
        Cliente cliente = crearCliente("Cliente Sin Lecturas");
        Sector sector = crearSector();
        crearMedidorConToma(cliente, sector);

        mockMvc.perform(post("/api/facturas/generar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generarFacturaJson(cliente.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void generarFacturaConVencimientoAnteriorAlPeriodoDevuelve400() throws Exception {
        Cliente cliente = crearCliente("Cliente Vencimiento Inválido");
        Sector sector = crearSector();
        Medidor medidor = crearMedidorConToma(cliente, sector);
        crearLectura(medidor, LocalDate.of(2024, 5, 10), 130, 30);
        crearTarifasVigentes();

        mockMvc.perform(post("/api/facturas/generar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(GenerarFacturaRequest.builder()
                                .clienteId(cliente.getId())
                                .periodoDesde(LocalDate.of(2024, 5, 1))
                                .periodoHasta(LocalDate.of(2024, 5, 31))
                                .vencimiento(LocalDate.of(2024, 5, 30))
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void generarFacturaConSobranteSobreUltimoTramoCobradoDevuelve201() throws Exception {
        Cliente cliente = crearCliente("Cliente Sobrante");
        Sector sector = crearSector();
        Medidor medidor = crearMedidorConToma(cliente, sector);
        crearLectura(medidor, LocalDate.of(2024, 5, 10), 240, 40);
        LocalDate vigenteDesde = LocalDate.of(2024, 1, 1);
        tarifaRepository.save(Tarifa.builder()
                .nombre("Tarifa Con Tope")
                .desdeM3(0)
                .hastaM3(10)
                .precioPorM3(new BigDecimal("1.5000"))
                .cargoFijo(new BigDecimal("12500.00"))
                .vigenteDesde(vigenteDesde)
                .build());
        tarifaRepository.save(Tarifa.builder()
                .nombre("Tarifa Con Tope 2")
                .desdeM3(11)
                .hastaM3(30)
                .precioPorM3(new BigDecimal("2.2500"))
                .vigenteDesde(vigenteDesde)
                .build());

        mockMvc.perform(post("/api/facturas/generar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generarFacturaJson(cliente.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.consumoM3").value(40))
                .andExpect(jsonPath("$.data.subtotal").value(82.50))
                .andExpect(jsonPath("$.data.total").value(12582.50));
    }

    @Test
    void generarFacturaTrasAnularLaAnteriorDevuelve201() throws Exception {
        Cliente cliente = crearCliente("Cliente Refactura");
        Sector sector = crearSector();
        Medidor medidor = crearMedidorConToma(cliente, sector);
        crearLectura(medidor, LocalDate.of(2024, 5, 10), 130, 30);
        crearTarifasVigentes();
        String body = generarFacturaJson(cliente.getId());

        String primera = mockMvc.perform(post("/api/facturas/generar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.numero").value("FAC-2024-05-0001"))
                .andReturn().getResponse().getContentAsString();

        long primeraId = objectMapper.readTree(primera).get("data").get("id").asLong();

        mockMvc.perform(put("/api/facturas/{id}/anular", primeraId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("ANULADA"));

        mockMvc.perform(post("/api/facturas/generar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.numero").value("FAC-2024-05-0002"))
                .andExpect(jsonPath("$.data.estado").value("PENDIENTE"));
    }

    @Test
    void obtenerFacturaConIdNoNumericoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/facturas/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void obtenerFacturaPorIdDevuelve200() throws Exception {
        Cliente cliente = crearCliente("Cliente Get");
        Factura factura = crearFactura(cliente, EstadoFactura.PENDIENTE,
                new BigDecimal("100.00"), LocalDate.of(2024, 6, 10));

        mockMvc.perform(get("/api/facturas/{id}", factura.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(factura.getId()))
                .andExpect(jsonPath("$.data.numero").value(factura.getNumero()))
                .andExpect(jsonPath("$.data.clienteId").value(cliente.getId()))
                .andExpect(jsonPath("$.data.total").value(100.00))
                .andExpect(jsonPath("$.data.estado").value("PENDIENTE"));
    }

    @Test
    void obtenerFacturaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/facturas/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listarFacturasPorClienteDevuelve200() throws Exception {
        Cliente cliente = crearCliente("Cliente Listado");
        crearFactura(cliente, EstadoFactura.PENDIENTE,
                new BigDecimal("100.00"), LocalDate.of(2024, 6, 10));

        mockMvc.perform(get("/api/facturas/cliente/{id}", cliente.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].clienteId").value(cliente.getId()))
                .andExpect(jsonPath("$.data[0].estado").value("PENDIENTE"));
    }

    @Test
    void anularFacturaDevuelve200() throws Exception {
        Cliente cliente = crearCliente("Cliente Anular");
        Factura factura = crearFactura(cliente, EstadoFactura.PENDIENTE,
                new BigDecimal("100.00"), LocalDate.of(2024, 6, 10));

        mockMvc.perform(put("/api/facturas/{id}/anular", factura.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(factura.getId()))
                .andExpect(jsonPath("$.data.estado").value("ANULADA"));
    }

    @Test
    void anularFacturaPagadaDevuelve409() throws Exception {
        Cliente cliente = crearCliente("Cliente Pagada");
        Factura factura = crearFactura(cliente, EstadoFactura.PAGADA,
                new BigDecimal("100.00"), LocalDate.of(2024, 6, 10));

        mockMvc.perform(put("/api/facturas/{id}/anular", factura.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void marcarFacturasVencidasDevuelve200() throws Exception {
        Cliente cliente = crearCliente("Cliente Vencido");
        crearFactura(cliente, EstadoFactura.PENDIENTE,
                new BigDecimal("100.00"), LocalDate.of(2020, 1, 1));

        mockMvc.perform(post("/api/facturas/marcar-vencidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void schedulerMarcaFacturasVencidas() {
        Cliente cliente = crearCliente("Cliente Scheduler");
        crearFactura(cliente, EstadoFactura.PENDIENTE,
                new BigDecimal("100.00"), LocalDate.of(2020, 1, 1));

        int marcadas = facturaVencimientoScheduler.marcarVencidas();

        assertTrue(marcadas >= 1);
    }

    @Test
    void schedulerMarcaVencidasAlArrancar() {
        Cliente cliente = crearCliente("Cliente Arranque");
        crearFactura(cliente, EstadoFactura.PENDIENTE,
                new BigDecimal("100.00"), LocalDate.of(2020, 1, 1));

        int marcadas = facturaVencimientoScheduler.marcarVencidasAlArrancar();

        assertTrue(marcadas >= 1);
    }
}