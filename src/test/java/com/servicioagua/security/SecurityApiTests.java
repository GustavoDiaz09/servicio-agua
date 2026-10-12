package com.servicioagua.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicioagua.security.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.seed.enabled=false")
@AutoConfigureMockMvc
@Transactional
class SecurityApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (!usuarioRepository.existsByUsername("tester")) {
            usuarioRepository.save(Usuario.builder()
                    .username("tester")
                    .password(passwordEncoder.encode("clave123"))
                    .rol(Rol.ADMIN)
                    .enabled(true)
                    .build());
        }
    }

    private String loginBody(String username, String password) throws Exception {
        LoginRequest login = LoginRequest.builder()
                .username(username)
                .password(password)
                .build();
        return objectMapper.writeValueAsString(login);
    }

    private String obtenerToken() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("tester", "clave123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.rol").value("ADMIN"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(response).path("data").path("token").asText();
        assertNotNull(token);
        assertFalse(token.isBlank());
        return token;
    }

    @Test
    void loginConCredencialesValidasDevuelve200() throws Exception {
        obtenerToken();
    }

    @Test
    void loginConCredencialesInvalidasDevuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("tester", "clave-incorrecta")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void loginDeUsuarioInexistenteDevuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("nadie", "clave123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void loginDeUsuarioDeshabilitadoDevuelve401() throws Exception {
        usuarioRepository.save(Usuario.builder()
                .username("deshabilitado")
                .password(passwordEncoder.encode("clave123"))
                .rol(Rol.ADMIN)
                .enabled(false)
                .build());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("deshabilitado", "clave123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void tokenDeUsuarioDeshabilitadoDevuelve401() throws Exception {
        String token = obtenerToken();
        Usuario usuario = usuarioRepository.findByUsername("tester").orElseThrow();
        usuario.setEnabled(false);
        usuarioRepository.save(usuario);
        mockMvc.perform(get("/api/clientes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void loginConCamposVaciosDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void accesoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void accesoConTokenInvalidoDevuelve401() throws Exception {
        mockMvc.perform(get("/api/clientes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void accesoConTokenValidoDevuelve200() throws Exception {
        String token = obtenerToken();
        mockMvc.perform(get("/api/clientes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void demasiadosIntentosFallidosDevuelve429() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("bloqueado", "clave-incorrecta")))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("bloqueado", "clave123")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false));
    }
}