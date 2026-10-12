package com.servicioagua.common.config;

import com.servicioagua.cliente.Cliente;
import com.servicioagua.cliente.ClienteRepository;
import com.servicioagua.common.enums.*;
import com.servicioagua.factura.Factura;
import com.servicioagua.factura.FacturaRepository;
import com.servicioagua.lectura.Lectura;
import com.servicioagua.lectura.LecturaRepository;
import com.servicioagua.medidor.Medidor;
import com.servicioagua.medidor.MedidorRepository;
import com.servicioagua.pago.Pago;
import com.servicioagua.pago.PagoRepository;
import com.servicioagua.red.Sector;
import com.servicioagua.red.SectorRepository;
import com.servicioagua.tarifa.Tarifa;
import com.servicioagua.tarifa.TarifaCalculadora;
import com.servicioagua.tarifa.TarifaRepository;
import com.servicioagua.toma.Toma;
import com.servicioagua.toma.TomaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private final SectorRepository sectorRepository;
    private final TarifaRepository tarifaRepository;
    private final TarifaCalculadora tarifaCalculadora;
    private final ClienteRepository clienteRepository;
    private final TomaRepository tomaRepository;
    private final MedidorRepository medidorRepository;
    private final LecturaRepository lecturaRepository;
    private final FacturaRepository facturaRepository;
    private final PagoRepository pagoRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (clienteRepository.count() > 0) {
            return;
        }

        Sector norte = sector("Sector Norte", "Zona A", "3.40");
        Sector centro = sector("Sector Centro", "Zona B", "3.10");
        Sector sur = sector("Sector Sur", "Zona C", "2.95");

        LocalDate hoy = LocalDate.now();
        int anio = hoy.getYear();
        int mes = hoy.getMonthValue();
        tarifa("Tarifa " + mes + "/" + anio + " base", 0, 10, "1.5000", "12500.00", hoy.minusYears(1), null);
        tarifa("Tarifa " + mes + "/" + anio + " tramo 2", 11, 30, "2.2500", "0.00", hoy.minusYears(1), null);
        tarifa("Tarifa " + mes + "/" + anio + " tramo 3", 31, 99999, "3.0000", "0.00", hoy.minusYears(1), null);

        Cliente perez = cliente("1036", "María Pérez", "3001234567", "maria.perez@correo.com",
                "Calle 10 #20-30", TipoCliente.RESIDENCIAL);
        Cliente lopez = cliente("1077444555", "Juan López", "3007654321", "juan.lopez@correo.com",
                "Carrera 5 #12-08", TipoCliente.RESIDENCIAL);
        Cliente garcia = cliente("1122334455", "Comercial García Ltda.", "3112223344", "ventas@garciacom.sa.com",
                "Av. Principal 45-12", TipoCliente.COMERCIAL);
        Cliente ruiz = cliente("8899667722", "Carlos Ruiz", "3201112233", "carlos.ruiz@correo.com",
                "Diagonal 8 #3-90", TipoCliente.RESIDENCIAL);
        Cliente colegio = cliente("9801234567", "Colegio San José", "3015556677", "admin@colegiosanjose.edu.co",
                "Calle 3 #1-02", TipoCliente.INSTITUCIONAL);

        Toma toma1 = toma("TOM-0001", norte, perez);
        Toma toma2 = toma("TOM-0002", centro, lopez);
        Toma toma3 = toma("TOM-0003", centro, garcia);
        Toma toma4 = toma("TOM-0004", sur, ruiz);
        Toma toma5 = toma("TOM-0005", norte, colegio);

        Medidor m1 = medidor("MTR-1001", toma1, hoy.minusMonths(14));
        Medidor m2 = medidor("MTR-1002", toma2, hoy.minusMonths(14));
        Medidor m3 = medidor("MTR-1003", toma3, hoy.minusMonths(14));
        Medidor m4 = medidor("MTR-1004", toma4, hoy.minusMonths(14));
        Medidor m5 = medidor("MTR-1005", toma5, hoy.minusMonths(14));

        Lectura l1Ant = lectura(m1, hoy.minusMonths(1), 120, 0);
        Lectura l1Act = lectura(m1, hoy, 150, 30);
        Lectura l2Ant = lectura(m2, hoy.minusMonths(1), 90, 0);
        Lectura l2Act = lectura(m2, hoy, 108, 18);
        Lectura l3Ant = lectura(m3, hoy.minusMonths(1), 250, 0);
        Lectura l3Act = lectura(m3, hoy, 320, 70);
        Lectura l4Ant = lectura(m4, hoy.minusMonths(1), 60, 0);
        Lectura l4Act = lectura(m4, hoy, 74, 14);
        Lectura l5Ant = lectura(m5, hoy.minusMonths(1), 400, 0);
        Lectura l5Act = lectura(m5, hoy, 470, 70);

        LocalDate periodoDesde = hoy.withDayOfMonth(1);
        Factura fPerez = factura(perez, l1Act, 30, periodoDesde, hoy, hoy.plusDays(20), EstadoFactura.PENDIENTE);
        Factura fGarcia = factura(garcia, l3Act, 70, periodoDesde, hoy, hoy.plusDays(12), EstadoFactura.PARCIAL);
        pago(fGarcia, new BigDecimal("3000.00"), MetodoPago.EFECTIVO);
        factura(lopez, l2Act, 18, periodoDesde, hoy, hoy.minusDays(5), EstadoFactura.VENCIDA);
    }

    private Factura factura(Cliente cliente, Lectura lectura, int consumo, LocalDate desde, LocalDate hasta,
                            LocalDate vencimiento, EstadoFactura estado) {
        List<Tarifa> tramos = tarifaRepository.findAll();
        BigDecimal cargoFijo = tarifaCalculadora.cargoFijo(tramos);
        BigDecimal subtotal = tarifaCalculadora.calcularSubtotal(consumo, tramos);
        String numero = String.format("FAC-%d-%02d-%04d", desde.getYear(), desde.getMonthValue(),
                facturaRepository.countByPeriodoDesde(desde) + 1);
        return facturaRepository.save(Factura.builder()
                .numero(numero)
                .cliente(cliente)
                .lectura(lectura)
                .consumoM3(consumo)
                .subtotal(subtotal)
                .cargoFijo(cargoFijo)
                .total(subtotal.add(cargoFijo))
                .periodoDesde(desde)
                .periodoHasta(hasta)
                .vencimiento(vencimiento)
                .estado(estado)
                .build());
    }

    private void pago(Factura factura, BigDecimal monto, MetodoPago metodo) {
        pagoRepository.save(Pago.builder()
                .factura(factura)
                .monto(monto)
                .fechaPago(LocalDate.now())
                .metodo(metodo)
                .build());
    }

    private Sector sector(String nombre, String zona, String presion) {
        return sectorRepository.save(Sector.builder()
                .nombre(nombre)
                .zona(zona)
                .presionPromedio(new BigDecimal(presion))
                .build());
    }

    private void tarifa(String nombre, int desde, int hasta, String precio, String cargoFijo,
                        LocalDate vigenteDesde, LocalDate vigenteHasta) {
        tarifaRepository.save(Tarifa.builder()
                .nombre(nombre)
                .desdeM3(desde)
                .hastaM3(hasta)
                .precioPorM3(new BigDecimal(precio))
                .cargoFijo(new BigDecimal(cargoFijo))
                .vigenteDesde(vigenteDesde)
                .vigenteHasta(vigenteHasta)
                .build());
    }

    private Cliente cliente(String doc, String nombre, String tel, String email, String dir,
                            TipoCliente tipo) {
        return clienteRepository.save(Cliente.builder()
                .documento(doc)
                .nombre(nombre)
                .telefono(tel)
                .email(email)
                .direccion(dir)
                .tipo(tipo)
                .estado(EstadoCliente.ACTIVO)
                .fechaRegistro(LocalDateTime.now())
                .build());
    }

    private Toma toma(String codigo, Sector sector, Cliente cliente) {
        return tomaRepository.save(Toma.builder()
                .codigo(codigo)
                .sector(sector)
                .cliente(cliente)
                .fechaAlta(LocalDate.now().minusMonths(13))
                .estado(EstadoToma.ACTIVA)
                .build());
    }

    private Medidor medidor(String serie, Toma toma, LocalDate instalacion) {
        return medidorRepository.save(Medidor.builder()
                .numeroSerie(serie)
                .toma(toma)
                .fechaInstalacion(instalacion)
                .estado(EstadoMedidor.ACTIVO)
                .build());
    }

    private Lectura lectura(Medidor medidor, LocalDate fecha, int valor, int consumo) {
        return lecturaRepository.save(Lectura.builder()
                .medidor(medidor)
                .valorLectura(valor)
                .consumoM3(consumo)
                .fechaLectura(fecha)
                .estado(EstadoLectura.ACTIVA)
                .build());
    }
}