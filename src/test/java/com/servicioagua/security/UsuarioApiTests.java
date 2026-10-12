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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
        crearRol(username, password, Rol.ADMIN);
    }

    private void crearRol(String username, String password, Rol rol) {
        usuarioRepository.save(Usuario.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .rol(rol)
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

    private String rolBody(String rol) throws Exception {
        return objectMapper.writeValueAsString(Map.of("rol", rol));
    }

    private void autenticarComoSuperadmin(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_SUPERADMIN"))));
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

    @Test
    void superadminEliminaUsuarioDevuelve200YLoginFalla() throws Exception {
        autenticarComoSuperadmin("venus");
        crearRol("venus", "clave1234", Rol.SUPERADMIN);
        crear("ana", "clave1234");
        mockMvc.perform(delete("/api/usuarios/{id}", idDe("ana")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("ana", "clave1234")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void eliminarUsuarioPorAdminDevuelve403() throws Exception {
        crear("ana", "clave1234");
        mockMvc.perform(delete("/api/usuarios/{id}", idDe("ana")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void eliminarUsuarioInexistenteDevuelve404() throws Exception {
        autenticarComoSuperadmin("venus");
        mockMvc.perform(delete("/api/usuarios/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void eliminarSuPropioUsuarioDevuelve409() throws Exception {
        autenticarComoSuperadmin("vega");
        crearRol("vega", "clave1234", Rol.SUPERADMIN);
        mockMvc.perform(delete("/api/usuarios/{id}", idDe("vega")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void eliminarUnicoSuperadminDevuelve409() throws Exception {
        autenticarComoSuperadmin("venus");
        crearRol("unica", "clave1234", Rol.SUPERADMIN);
        mockMvc.perform(delete("/api/usuarios/{id}", idDe("unica")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void crearSegundoSuperadminDevuelve409() throws Exception {
        autenticarComoSuperadmin("venus");
        crearRol("venus", "clave1234", Rol.SUPERADMIN);
        crear("ana", "clave1234");
        mockMvc.perform(put("/api/usuarios/{id}/rol", idDe("ana"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rolBody("SUPERADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe un superadministrador; solo puede existir uno"));
    }

    @Test
    void superadminPromueveCuandoNoExisteOtroDevuelve200() throws Exception {
        autenticarComoSuperadmin("venus");
        crear("ana", "clave1234");
        mockMvc.perform(put("/api/usuarios/{id}/rol", idDe("ana"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rolBody("SUPERADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rol").value("SUPERADMIN"));
        mockMvc.perform(put("/api/usuarios/{id}/rol", idDe("ana"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rolBody("ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void cambiarRolPorAdminDevuelve403() throws Exception {
        crear("ana", "clave1234");
        mockMvc.perform(put("/api/usuarios/{id}/rol", idDe("ana"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rolBody("SUPERADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void cambiarRolUsuarioInexistenteDevuelve404() throws Exception {
        autenticarComoSuperadmin("venus");
        mockMvc.perform(put("/api/usuarios/9999/rol")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rolBody("SUPERADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void cambiarRolSuPropioUsuarioDevuelve409() throws Exception {
        autenticarComoSuperadmin("vega");
        crearRol("vega", "clave1234", Rol.SUPERADMIN);
        mockMvc.perform(put("/api/usuarios/{id}/rol", idDe("vega"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rolBody("ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void quitarRolUnicoSuperadminDevuelve409() throws Exception {
        autenticarComoSuperadmin("venus");
        crearRol("unica", "clave1234", Rol.SUPERADMIN);
        mockMvc.perform(put("/api/usuarios/{id}/rol", idDe("unica"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rolBody("ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void superadminTambienPuedeGestionarEstadoYPassword() throws Exception {
        autenticarComoSuperadmin("venus");
        crearRol("venus", "clave1234", Rol.SUPERADMIN);
        crear("ana", "clave1234");
        mockMvc.perform(put("/api/usuarios/{id}/estado", idDe("ana"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estadoBody(false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));
        mockMvc.perform(put("/api/usuarios/{id}/password", idDe("ana"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody("reseteada9")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void adminNoPuedeDeshabilitarSuperadminDevuelve403() throws Exception {
        crearRol("root", "clave1234", Rol.SUPERADMIN);
        crear("ana", "clave1234");
        mockMvc.perform(put("/api/usuarios/{id}/estado", idDe("root"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(estadoBody(false)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void adminNoPuedeResetearPasswordDeSuperadminDevuelve403() throws Exception {
        crearRol("root", "clave1234", Rol.SUPERADMIN);
        mockMvc.perform(put("/api/usuarios/{id}/password", idDe("root"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody("reseteada9")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void superadminPuedeResetearPasswordDeOtroSuperadmin() throws Exception {
        autenticarComoSuperadmin("venus");
        crearRol("venus", "clave1234", Rol.SUPERADMIN);
        crearRol("root", "clave1234", Rol.SUPERADMIN);
        mockMvc.perform(put("/api/usuarios/{id}/password", idDe("root"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody("reseteada9")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}