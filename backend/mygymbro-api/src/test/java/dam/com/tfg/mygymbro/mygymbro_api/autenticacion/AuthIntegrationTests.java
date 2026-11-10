package dam.com.tfg.mygymbro.mygymbro_api.autenticacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.Clock;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder encoder;
    @MockitoBean CorreoConfirmacion correo;
    @MockitoBean GoogleIdentityVerifier google;
    @MockitoBean Clock clock;

    @BeforeEach
    void limpiar() {
        when(clock.instant()).thenAnswer(invocation -> Instant.now());
        jdbc.update("DELETE FROM verificaciones_correo");
        jdbc.update("DELETE FROM sesiones");
        jdbc.update("DELETE FROM usuario_roles");
        jdbc.update("DELETE FROM usuarios");
    }

    @Test
    void registroConAmbosRolesSinExponerContrasena() throws Exception {
        registrar("ANA@example.com", "[\"CLIENTE\",\"ENTRENADOR\"]");
        String hash = jdbc.queryForObject("SELECT contrasena_hash FROM usuarios", String.class);
        assertThat(hash).isNotEqualTo("ClaveSegura123");
        assertThat(hash).startsWith("{bcrypt}");
        assertThat(encoder.matches("ClaveSegura123", hash)).isTrue();
        assertThat(jdbc.queryForObject("SELECT email FROM usuarios", String.class)).isEqualTo("ana@example.com");
        String token = login("ana@example.com", "ClaveSegura123");
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", containsInAnyOrder("CLIENTE", "ENTRENADOR")))
                .andExpect(jsonPath("$.contrasenaHash").doesNotExist());
        String almacenado = jdbc.queryForObject("SELECT token_hash FROM sesiones", String.class);
        assertThat(almacenado).isNotEqualTo(token).isEqualTo(AuthService.hashToken(token));
    }

    @Test
    void registroAceptaCadaRolIndividual() throws Exception {
        registrar("cliente@example.com", "[\"CLIENTE\"]");
        registrar("entrenador@example.com", "[\"ENTRENADOR\"]");
    }

    @Test
    void correoDuplicadoSinDistinguirMayusculas() throws Exception {
        registrar("ana@example.com", "[\"CLIENTE\"]");
        mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(registroJson("ANA@example.com", "[\"CLIENTE\"]")))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class)).isEqualTo(1);
    }

    @Test
    void datosInvalidosNoCreanUsuarios() throws Exception {
        for (String body : new String[] {
                registroJson("correo-invalido", "[\"CLIENTE\"]"),
                registroJson("ana@example.com", "[]"),
                registroJson("ana@example.com", "[\"ADMIN\"]"),
                registroJson("ana@example.com", "[\"NUTRICIONISTA\"]"),
                registroJson("ana@example.com", "[null]"),
                registroJson("ana@example.com", "[\"CLIENTE\"]").replace("ClaveSegura123", "corta"),
                registroJson("ana@example.com", "[\"CLIENTE\"]").replace("Ana", " ")
        }) {
            mvc.perform(post("/api/auth/register").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{mal"))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class)).isZero();
    }

    @Test
    void credencialesInvalidasDanElMismoError() throws Exception {
        registrar("ana@example.com", "[\"CLIENTE\"]");
        for (String email : new String[] {"ana@example.com", "nadie@example.com"}) {
            mvc.perform(post("/api/auth/login").contentType("application/json")
                            .content(json.writeValueAsString(new AuthDtos.Login(email, "incorrecta"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.mensaje").value("Credenciales incorrectas"));
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sesiones", Integer.class)).isZero();
    }

    @Test
    void logoutRevocaSoloLaSesionActual() throws Exception {
        registrar("ana@example.com", "[\"CLIENTE\"]");
        String primera = login("ana@example.com", "ClaveSegura123");
        String segunda = login("ana@example.com", "ClaveSegura123");
        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + primera))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + primera))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + segunda))
                .andExpect(status().isOk());
    }

    @Test
    void sesionesSinCaducidadSiguenValidasHastaCerrarSesion() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer inventado"))
                .andExpect(status().isUnauthorized());
        registrar("ana@example.com", "[\"CLIENTE\"]");
        String token = login("ana@example.com", "ClaveSegura123");
        assertThat(jdbc.queryForObject("SELECT expira_en FROM sesiones", Timestamp.class)).isNull();
        when(clock.instant()).thenReturn(Instant.now().plusSeconds(10L * 365 * 24 * 60 * 60));
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private void registrar(String email, String roles) throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json").content(registroJson(email, roles)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").isString())
                .andExpect(jsonPath("$.contrasena").doesNotExist())
                .andExpect(jsonPath("$.contrasenaHash").doesNotExist());
        var captor = ArgumentCaptor.forClass(String.class);
        verify(correo).enviar(eq(email.toLowerCase(java.util.Locale.ROOT)), captor.capture());
        mvc.perform(post("/api/auth/confirm-email").contentType("application/json")
                        .content(json.writeValueAsString(new AuthDtos.Confirmacion(captor.getValue()))))
                .andExpect(status().isOk());
    }

    @Test
    void noPermiteEntrarAntesDeConfirmarYElEnlaceSoloSeUsaUnaVez() throws Exception {
        String token = registrarPendiente();
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content(json.writeValueAsString(new AuthDtos.Login("ana@example.com", "ClaveSegura123"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/verify-email").param("token", token))
                .andExpect(status().isOk()).andExpect(header().string("Referrer-Policy", "no-referrer"));
        assertThat(jdbc.queryForObject("SELECT email_verificado FROM usuarios", Boolean.class)).isFalse();
        mvc.perform(post("/api/auth/verify-email").contentType("application/x-www-form-urlencoded").param("token", token))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/confirm-email").contentType("application/json")
                        .content(json.writeValueAsString(new AuthDtos.Confirmacion(token))))
                .andExpect(status().isBadRequest());
        login("ana@example.com", "ClaveSegura123");
    }

    @Test
    void enlaceCaducadoYReenvioConEsperaInvalidanElAnterior() throws Exception {
        String anterior = registrarPendiente();
        mvc.perform(post("/api/auth/resend-verification").contentType("application/json")
                        .content("{\"email\":\"ana@example.com\"}"))
                .andExpect(status().isOk());
        verify(correo, times(1)).enviar(anyString(), anyString());
        when(clock.instant()).thenReturn(Instant.now().plusSeconds(25 * 60 * 60));
        mvc.perform(post("/api/auth/confirm-email").contentType("application/json")
                        .content(json.writeValueAsString(new AuthDtos.Confirmacion(anterior))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/resend-verification").contentType("application/json")
                        .content("{\"email\":\"ana@example.com\"}"))
                .andExpect(status().isOk());
        var captor = ArgumentCaptor.forClass(String.class);
        verify(correo, times(2)).enviar(eq("ana@example.com"), captor.capture());
        String nuevo = captor.getAllValues().getLast();
        assertThat(nuevo).isNotEqualTo(anterior);
        mvc.perform(post("/api/auth/confirm-email").contentType("application/json")
                        .content(json.writeValueAsString(new AuthDtos.Confirmacion(anterior))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/confirm-email").contentType("application/json")
                        .content(json.writeValueAsString(new AuthDtos.Confirmacion(nuevo))))
                .andExpect(status().isOk());
    }

    @Test
    void falloDeCorreoRevierteRegistroSinAnunciarExito() throws Exception {
        doThrow(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "No se ha podido enviar el correo"))
                .when(correo).enviar(anyString(), anyString());
        mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(registroJson("ana@example.com", "[\"CLIENTE\"]")))
                .andExpect(status().isServiceUnavailable());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM verificaciones_correo", Integer.class)).isZero();
    }

    @Test
    void bcryptRechazaMasDe72BytesSinTruncarLaContrasena() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(registroJson("ana@example.com", "[\"CLIENTE\"]").replace("ClaveSegura123", "ñ".repeat(37))))
                .andExpect(status().isBadRequest());
        registrar("ana@example.com", "[\"CLIENTE\"]");
        mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content(json.writeValueAsString(new AuthDtos.Login("ana@example.com", "ñ".repeat(37)))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actualizaHashAnteriorSinCambiarContrasena() throws Exception {
        registrar("ana@example.com", "[\"CLIENTE\"]");
        String antiguo = "{pbkdf2}" + org.springframework.security.crypto.password.Pbkdf2PasswordEncoder
                .defaultsForSpringSecurity_v5_8().encode("ClaveSegura123");
        jdbc.update("UPDATE usuarios SET contrasena_hash = ?", antiguo);
        login("ana@example.com", "ClaveSegura123");
        assertThat(jdbc.queryForObject("SELECT contrasena_hash FROM usuarios", String.class)).startsWith("{bcrypt}");
    }

    @Test
    void googleCreaCuentaYUsaSubSinCambiarRolesEnCadaAcceso() throws Exception {
        when(google.verificar("valido")).thenReturn(new GoogleIdentityVerifier.Identidad("google-1", "ana@gmail.com", "Ana"));
        String body = mvc.perform(post("/api/auth/google").contentType("application/json")
                        .content("{\"idToken\":\"valido\",\"roles\":[\"CLIENTE\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usuario.roles", containsInAnyOrder("CLIENTE")))
                .andReturn().getResponse().getContentAsString();
        assertThat(jdbc.queryForObject("SELECT contrasena_hash FROM usuarios", String.class)).isNull();
        when(google.verificar("valido")).thenReturn(new GoogleIdentityVerifier.Identidad("google-1", "otro@gmail.com", "Ana"));
        mvc.perform(post("/api/auth/google").contentType("application/json")
                        .content("{\"idToken\":\"valido\",\"roles\":[\"ENTRENADOR\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usuario.roles", containsInAnyOrder("CLIENTE")));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class)).isEqualTo(1);
        String token = json.readTree(body).get("accessToken").asString();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }

    @Test
    void googleNoUneCuentasSoloPorCorreoPeroPermiteVincularConSesion() throws Exception {
        registrar("ana@example.com", "[\"CLIENTE\"]");
        when(google.verificar("valido")).thenReturn(new GoogleIdentityVerifier.Identidad("google-1", "ana@example.com", "Ana"));
        mvc.perform(post("/api/auth/google").contentType("application/json")
                        .content("{\"idToken\":\"valido\",\"roles\":[\"CLIENTE\"]}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/google/link").contentType("application/json").content("{\"idToken\":\"valido\"}"))
                .andExpect(status().isUnauthorized());
        String sesion = login("ana@example.com", "ClaveSegura123");
        mvc.perform(post("/api/auth/google/link").header("Authorization", "Bearer " + sesion)
                        .contentType("application/json").content("{\"idToken\":\"valido\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/google").contentType("application/json").content("{\"idToken\":\"valido\"}"))
                .andExpect(status().isOk());
    }

    private String registrarPendiente() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json")
                        .content(registroJson("ana@example.com", "[\"CLIENTE\"]")))
                .andExpect(status().isCreated());
        var captor = ArgumentCaptor.forClass(String.class);
        verify(correo).enviar(eq("ana@example.com"), captor.capture());
        assertThat(jdbc.queryForObject("SELECT token_hash FROM verificaciones_correo", String.class))
                .isEqualTo(AuthService.hashToken(captor.getValue())).isNotEqualTo(captor.getValue());
        return captor.getValue();
    }

    private String login(String email, String contrasena) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content(json.writeValueAsString(new AuthDtos.Login(email, contrasena))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.usuario.contrasenaHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("accessToken").asString();
    }

    private String registroJson(String email, String roles) {
        return "{\"nombre\":\"Ana\",\"email\":\"" + email
                + "\",\"contrasena\":\"ClaveSegura123\",\"roles\":" + roles + "}";
    }
}
