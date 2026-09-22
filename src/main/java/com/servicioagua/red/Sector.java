package com.servicioagua.red;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sector", uniqueConstraints = @UniqueConstraint(columnNames = "nombre"))
public class Sector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @jakarta.validation.constraints.NotBlank
    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 120)
    private String zona;

    @Column(precision = 6, scale = 2)
    private BigDecimal presionPromedio;
}