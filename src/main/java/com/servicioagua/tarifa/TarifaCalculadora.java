package com.servicioagua.tarifa;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Component
public class TarifaCalculadora {

    private static final int CENTAVOS = 2;

    public BigDecimal calcularSubtotal(int consumoTotal, List<Tarifa> tramos) {
        List<Tarifa> ordenados = tramos.stream()
                .sorted(Comparator.comparingInt(Tarifa::getDesdeM3))
                .toList();
        BigDecimal subtotal = BigDecimal.ZERO;
        int restante = consumoTotal;
        for (Tarifa tramo : ordenados) {
            if (restante <= 0) {
                break;
            }
            int capacidad = tramo.getDesdeM3() == 0
                    ? tramo.getHastaM3()
                    : tramo.getHastaM3() - tramo.getDesdeM3() + 1;
            int m3DelTramo = Math.min(restante, capacidad);
            subtotal = subtotal.add(tramo.getPrecioPorM3().multiply(BigDecimal.valueOf(m3DelTramo)));
            restante -= m3DelTramo;
        }
        if (restante > 0) {
            BigDecimal precioTop = ordenados.get(ordenados.size() - 1).getPrecioPorM3();
            subtotal = subtotal.add(precioTop.multiply(BigDecimal.valueOf(restante)));
        }
        return subtotal.setScale(CENTAVOS, RoundingMode.HALF_UP);
    }

    public BigDecimal cargoFijo(List<Tarifa> tramos) {
        return tramos.stream()
                .filter(t -> t.getDesdeM3() == 0)
                .map(Tarifa::getCargoFijo)
                .map(cargo -> cargo != null ? cargo : BigDecimal.ZERO)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }
}
