package com.servicioagua.reporte;

import com.servicioagua.cliente.Cliente;
import com.servicioagua.cliente.ClienteRepository;
import com.servicioagua.common.enums.EstadoCliente;
import com.servicioagua.common.enums.EstadoFactura;
import com.servicioagua.common.enums.EstadoFuga;
import com.servicioagua.common.enums.EstadoLectura;
import com.servicioagua.common.enums.EstadoMedidor;
import com.servicioagua.common.enums.EstadoToma;
import com.servicioagua.common.enums.MetodoPago;
import com.servicioagua.common.enums.SeveridadFuga;
import com.servicioagua.common.enums.TipoCliente;
import com.servicioagua.factura.Factura;
import com.servicioagua.factura.FacturaRepository;
import com.servicioagua.lectura.Lectura;
import com.servicioagua.lectura.LecturaRepository;
import com.servicioagua.medidor.Medidor;
import com.servicioagua.medidor.MedidorRepository;
import com.servicioagua.pago.Pago;
import com.servicioagua.pago.PagoRepository;
import com.servicioagua.red.Fuga;
import com.servicioagua.red.FugaRepository;
import com.servicioagua.red.Sector;
import com.servicioagua.red.SectorRepository;
import com.servicioagua.toma.Toma;
import com.servicioagua.toma.TomaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
@Transactional
class ReporteApiTests {

    @Autowired
    private MockMvc mockMvc;

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
    private FacturaRepository facturaRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private FugaRepository fugaRepository;

    private int secuencia;

    private Cliente crearCliente() {
        return clienteRepository.save(Cliente.builder()
                .documento("DOC-REP-" + ++secuencia)
                .nombre("Cliente Reporte")
                .telefono("3000000000")
                .email("reporte@correo.com")
                .direccion("Calle 1 #2-3")
                .tipo(TipoCliente.RESIDENCIAL)
                .estado(EstadoCliente.ACTIVO)
                .build());
    }

    private Sector crearSector() {
        return sectorRepository.save(Sector.builder()
                .nombre("Sector Reporte " + ++secuencia)
                .zona("Zona Z")
                .presionPromedio(new BigDecimal("3.00"))
                .build());
    }

    private Medidor crearMedidorConToma(Cliente cliente, Sector sector) {
        Toma toma = tomaRepository.save(Toma.builder()
                .codigo("TOM-REP-" + secuencia)
                .sector(sector)
                .cliente(cliente)
                .fechaAlta(LocalDate.of(2023, 1, 1))
                .estado(EstadoToma.ACTIVA)
                .build());
        return medidorRepository.save(Medidor.builder()
                .numeroSerie("MTR-REP-" + secuencia)
                .toma(toma)
                .fechaInstalacion(LocalDate.of(2023, 1, 1))
                .estado(EstadoMedidor.ACTIVO)
                .build());
    }

    private Lectura crearLectura(Medidor medidor, int consumo) {
        return lecturaRepository.save(Lectura.builder()
                .medidor(medidor)
                .valorLectura(500 + consumo)
                .consumoM3(consumo)
                .fechaLectura(LocalDate.of(2024, 5, 10))
                .estado(EstadoLectura.ACTIVA)
                .build());
    }

    private Factura crearFactura(Cliente cliente, EstadoFactura estado, BigDecimal total, LocalDate vencimiento) {
        return facturaRepository.save(Factura.builder()
                .numero("FAC-REP-" + secuencia)
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

    @Test
    void consumoPorSectorDevuelve200ConConsumoAcumulado() throws Exception {
        Cliente cliente = crearCliente();
        Sector sector = crearSector();
        crearLectura(crearMedidorConToma(cliente, sector), 40);

        mockMvc.perform(get("/api/reportes/consumo-por-sector")
                        .param("desde", "2024-01-01")
                        .param("hasta", "2024-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].sector").value(sector.getId().intValue()))
                .andExpect(jsonPath("$.data[0].nombre").value(sector.getNombre()))
                .andExpect(jsonPath("$.data[0].consumoTotal").value(40));
    }

    @Test
    void ventasPorPeriodoDevuelve200ConMontoAgregado() throws Exception {
        Cliente cliente = crearCliente();
        Factura factura = crearFactura(cliente, EstadoFactura.PENDIENTE,
                new BigDecimal("100.00"), LocalDate.of(2024, 6, 10));
        pagoRepository.save(Pago.builder()
                .factura(factura)
                .monto(new BigDecimal("100.00"))
                .fechaPago(LocalDate.of(2024, 5, 10))
                .metodo(MetodoPago.EFECTIVO)
                .build());
        pagoRepository.save(Pago.builder()
                .factura(factura)
                .monto(new BigDecimal("30.00"))
                .fechaPago(LocalDate.of(2024, 5, 20))
                .metodo(MetodoPago.TRANSFERENCIA)
                .build());

        mockMvc.perform(get("/api/reportes/ventas-por-periodo")
                        .param("desde", "2024-05-01")
                        .param("hasta", "2024-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].periodo").value("2024-05"))
                .andExpect(jsonPath("$.data[0].totalVendido").value(130.00));
    }

    @Test
    void clientesMorososDevuelve200ConFacturaVencida() throws Exception {
        Cliente cliente = crearCliente();
        crearFactura(cliente, EstadoFactura.PENDIENTE,
                new BigDecimal("1234.56"), LocalDate.of(2020, 1, 1));

        mockMvc.perform(get("/api/reportes/clientes-morosos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].clienteId").value(cliente.getId().intValue()))
                .andExpect(jsonPath("$.data[0].documento").value(cliente.getDocumento()))
                .andExpect(jsonPath("$.data[0].nombre").value(cliente.getNombre()))
                .andExpect(jsonPath("$.data[0].deudaTotal").value(1234.56))
                .andExpect(jsonPath("$.data[0].cantidadFacturas").value(1))
                .andExpect(jsonPath("$.data[0].facturas[0].numero").value("FAC-REP-" + secuencia))
                .andExpect(jsonPath("$.data[0].facturas[0].estado").value("PENDIENTE"));
    }

    @Test
    void fugasActivasDevuelve200ConSoloNoResueltas() throws Exception {
        Sector sector = crearSector();
        fugaRepository.save(Fuga.builder()
                .sector(sector)
                .ubicacion("Fuga reportada")
                .severidad(SeveridadFuga.ALTA)
                .estado(EstadoFuga.REPORTADA)
                .fechaReporte(LocalDate.of(2024, 5, 1))
                .build());
        fugaRepository.save(Fuga.builder()
                .sector(sector)
                .ubicacion("Fuga en curso")
                .severidad(SeveridadFuga.MEDIA)
                .estado(EstadoFuga.EN_CURSO)
                .fechaReporte(LocalDate.of(2024, 5, 2))
                .build());
        fugaRepository.save(Fuga.builder()
                .sector(sector)
                .ubicacion("Fuga resuelta")
                .severidad(SeveridadFuga.BAJA)
                .estado(EstadoFuga.RESUELTA)
                .fechaReporte(LocalDate.of(2024, 5, 3))
                .fechaResolucion(LocalDate.of(2024, 5, 4))
                .build());

        mockMvc.perform(get("/api/reportes/fugas-activas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].estado").value("REPORTADA"))
                .andExpect(jsonPath("$.data[1].estado").value("EN_CURSO"));
    }
}