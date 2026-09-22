package com.servicioagua.pago;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicioagua.cliente.Cliente;
import com.servicioagua.cliente.ClienteRepository;
import com.servicioagua.common.enums.EstadoCliente;
import com.servicioagua.common.enums.EstadoFactura;
import com.servicioagua.common.enums.MetodoPago;
import com.servicioagua.common.enums.TipoCliente;
import com.servicioagua.factura.Factura;
import com.servicioagua.factura.FacturaRepository;
import com.servicioagua.pago.dto.RegistrarPagoRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
@Transactional
class PagoApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private FacturaRepository facturaRepository;

    private int secuencia;

    private Cliente crearCliente() {
        return clienteRepository.save(Cliente.builder()
                .documento("DOC-PAGO-" + ++secuencia)
                .nombre("Cliente Pago")
                .telefono("3000000000")
                .email("pago@correo.com")
                .direccion("Calle 1 #2-3")
                .tipo(TipoCliente.RESIDENCIAL)
                .estado(EstadoCliente.ACTIVO)
                .build());
    }

    private Factura crearFactura(EstadoFactura estado, BigDecimal total) {
        Cliente cliente = crearCliente();
        return facturaRepository.save(Factura.builder()
                .numero("FAC-PAGO-" + secuencia)
                .cliente(cliente)
                .consumoM3(30)
                .subtotal(total)
                .cargoFijo(BigDecimal.ZERO)
                .total(total)
                .periodoDesde(LocalDate.of(2024, 5, 1))
                .periodoHasta(LocalDate.of(2024, 5, 31))
                .vencimiento(LocalDate.of(2030, 6, 10))
                .estado(estado)
                .build());
    }

    private String pagoJson(Long facturaId, BigDecimal monto) throws Exception {
        return objectMapper.writeValueAsString(RegistrarPagoRequest.builder()
                .facturaId(facturaId)
                .monto(monto)
                .metodo(MetodoPago.EFECTIVO)
                .referencia("PAGO-TEST-" + ++secuencia)
                .build());
    }

    @Test
    void pagoTotalDevuelve201YFacturaQuedaPagada() throws Exception {
        Factura factura = crearFactura(EstadoFactura.PENDIENTE, new BigDecimal("100.00"));

        mockMvc.perform(post("/api/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pagoJson(factura.getId(), new BigDecimal("100.00"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.facturaNumero").value(factura.getNumero()))
                .andExpect(jsonPath("$.data.estadoFactura").value("PAGADA"));

        mockMvc.perform(get("/api/facturas/{id}", factura.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("PAGADA"));
    }

    @Test
    void pagoParcialDevuelve201YEstadoParcial() throws Exception {
        Factura factura = crearFactura(EstadoFactura.PENDIENTE, new BigDecimal("100.00"));

        mockMvc.perform(post("/api/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pagoJson(factura.getId(), new BigDecimal("30.00"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.monto").value(30.00))
                .andExpect(jsonPath("$.data.estadoFactura").value("PARCIAL"));

        mockMvc.perform(get("/api/facturas/{id}", factura.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estado").value("PARCIAL"));
    }

    @Test
    void pagoMayorADeudaPendienteDevuelve400() throws Exception {
        Factura factura = crearFactura(EstadoFactura.PENDIENTE, new BigDecimal("100.00"));

        mockMvc.perform(post("/api/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pagoJson(factura.getId(), new BigDecimal("150.00"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void pagoSobreFacturaPagadaDevuelve409() throws Exception {
        Factura factura = crearFactura(EstadoFactura.PAGADA, new BigDecimal("100.00"));

        mockMvc.perform(post("/api/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pagoJson(factura.getId(), new BigDecimal("10.00"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void pagoAFacturaInexistenteDevuelve404() throws Exception {
        mockMvc.perform(post("/api/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pagoJson(999999L, new BigDecimal("10.00"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}