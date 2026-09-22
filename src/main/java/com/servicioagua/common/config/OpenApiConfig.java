package com.servicioagua.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI servicioAguaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistema de Gestión de Agua Potable API")
                        .description("API REST para la gestión de clientes, medidores, lecturas, " +
                                "tarifas, facturación, pagos e infraestructura de red.")
                        .version("1.0.0")
                        .contact(new Contact().name("Equipo Servicio de Agua")));
    }
}