package com.servicioagua.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"app.seed.enabled=false", "app.security.admin-auto-create=false"})
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class UsuarioApiTests {

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
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void crear(String username, String password) {
        usuarioRepository.save(Usuario.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .rol(Rol.ADMIN)
                .enabled(true)
                .build());
    }

    private Long idDe(String username) {
        return usuarioRepository.findByUsername(username).orElseThrow().getId();
    }

    private String crearUsuarioBody(String username, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of("username", username, "password", password));
    }

    private String estadoBody(boolean enabled) throws Exception {
        return objectMapper.writeValueAsString(Map.of("enabled", enabled));
    }

    private String resetBody(String newPassword) throws Exception {
        return objectMapper.writeValueAsString(Map.of("newPassword", newPassword));
    }

    private String cambioPasswordBody(String current, String next) throws Exception {
        return objectMapper.writeValueAsString(Map.of("currentPassword", current, "newPassword", next));
    }

    private String loginBody(String username, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of("username", username, "password", password));
    }

    @Test
    void listarUsuariosDevuelve200() throws Exception {
        crear("juan", "clave1234");
        crear("pedro", "clave1234");
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void crearUsuarioDevuelve201() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearUsuarioBody("ana", "clave1234")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("ana"))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void crearUsuarioDuplicadoDevuelve409() throws Exception {
        crear("ana", "clave1234");
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearUsuarioBody("ana", "clave1234")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void crearUsuarioPasswordCortaDevuelve400() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearUsuarioBody("ana", "corta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void deshabilitarAdminNoUnicoDevuelve200() throws Exception {
        crear("juan", "clave1234");
        crear("pedro", "clave1234");
        mockMvc.perform(put("/api/usuarios/{id}/estado", idDe("pedro"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estadoBody(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    void habilitarUsuarioDevuelve200() throws Exception {
        crear("juan", "clave1234");
        mockMvc.perform(put("/api/usuarios/{id}/estado", idDe("juan"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estadoBody(true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void deshabilitarUnicoAdminDevuelve409() throws Exception {
        crear("juan", "clave1234");
        mockMvc.perform(put("/api/usuarios/{id}/estado", idDe("juan"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estadoBody(false)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void deshabilitarseActivoDevuelve409() throws Exception {
        crear("juan", "clave1234");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("juan", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        mockMvc.perform(put("/api/usuarios/{id}/estado", idDe("juan"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estadoBody(false)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void cambiarEstadoUsuarioInexistenteDevuelve404() throws Exception {
        mockMvc.perform(put("/api/usuarios/9999/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estadoBody(false)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void resetPasswordDevuelve200YLoginFuncionaConLaNueva() throws Exception {
        crear("juan", "clave1234");
        mockMvc.perform(put("/api/usuarios/{id}/password", idDe("juan"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody("reseteada9")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("juan", "reseteada9")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void cambiarMiPasswordDevuelve200YLoginFuncionaConLaNueva() throws Exception {
        crear("juan", "clave1234");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("juan", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        mockMvc.perform(put("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cambioPasswordBody("clave1234", "nueva12345")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("juan", "nueva12345")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void cambiarMiPasswordConClaveActualIncorrectaDevuelve409() throws Exception {
        crear("juan", "clave1234");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("juan", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        mockMvc.perform(put("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cambioPasswordBody("incorrecta1", "nueva12345")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void cambiarMiPasswordPasswordCortaDevuelve400() throws Exception {
        crear("juan", "clave1234");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("juan", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        mockMvc.perform(put("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cambioPasswordBody("clave1234", "corta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}