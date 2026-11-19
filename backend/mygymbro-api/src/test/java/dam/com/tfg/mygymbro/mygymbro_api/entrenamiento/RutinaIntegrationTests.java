package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento;

import dam.com.tfg.mygymbro.mygymbro_api.autenticacion.AuthDtos.UsuarioRespuesta;
import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.repository.RutinaRepository;
import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.dto.CrearRutinaDTO;
import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.service.RutinaService;
import dam.com.tfg.mygymbro.mygymbro_api.usuario.RolRepository;
import dam.com.tfg.mygymbro.mygymbro_api.usuario.Usuario;
import dam.com.tfg.mygymbro.mygymbro_api.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.List;
import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.dto.EntrenamientoDtos.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:rutinas_integration;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.url=jdbc:h2:mem:rutinas_integration;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.flyway.user=sa",
        "spring.flyway.password=",
        "spring.flyway.locations=classpath:db/migration"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RutinaIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired RolRepository roles;
    @Autowired RutinaRepository rutinas;
    @Autowired RutinaService service;
    @Autowired PasswordEncoder encoder;
    @Autowired jakarta.persistence.EntityManager entityManager;

    private Usuario propietario;
    private Usuario otro;
    private String token;

    @BeforeEach
    void prepararUsuariosYSesion() throws Exception {
        String hash = encoder.encode("ClaveDePrueba123");
        String sufijo = java.util.UUID.randomUUID().toString();
        propietario = crearUsuario("propietario-" + sufijo + "@example.com", hash);
        otro = crearUsuario("otro-" + sufijo + "@example.com", hash);
        token = login(propietario);
    }

    @Test
    void crearDevuelve201IdYNombreYGuardaElPropietarioAutenticado() throws Exception {
        var respuesta = mvc.perform(post("/api/rutina/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"nombre\":\"Torso/Pierna\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.nombre").value("Torso/Pierna"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.usuario").doesNotExist())
                .andReturn();

        int id = json.readTree(respuesta.getResponse().getContentAsString()).get("id").asInt();
        Rutina guardada = rutinas.findById(id).orElseThrow();
        assertThat(guardada.getNombre()).isEqualTo("Torso/Pierna");
        assertThat(guardada.getUsuario().getId()).isEqualTo(propietario.getId());
        assertThat(guardada.isEliminada()).isFalse();
    }

    @Test
    void listarDevuelveVariasPropiasSinEliminadasNiAjenas() throws Exception {
        Rutina primera = rutinas.saveAndFlush(new Rutina("Torso", propietario));
        Rutina segunda = rutinas.saveAndFlush(new Rutina("Pierna", propietario));
        rutinas.saveAndFlush(new Rutina("Ajena", otro));
        Rutina eliminada = new Rutina("Eliminada", propietario);
        eliminada.eliminar();
        rutinas.saveAndFlush(eliminada);

        mvc.perform(get("/api/rutina").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(primera.getId(), segunda.getId())))
                .andExpect(jsonPath("$[*].nombre", containsInAnyOrder("Torso", "Pierna")));

        mvc.perform(get("/api/rutina").header("Authorization", "Bearer " + login(otro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Ajena"));
    }

    @Test
    void sinRutinasPropiasDevuelve200YArrayVacio() throws Exception {
        rutinas.saveAndFlush(new Rutina("Solo del otro", otro));
        mvc.perform(get("/api/rutina").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void todasEliminadasDevuelveArrayVacio() throws Exception {
        Rutina eliminada = new Rutina("Antigua", propietario);
        eliminada.eliminar();
        rutinas.saveAndFlush(eliminada);
        mvc.perform(get("/api/rutina").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void nombresInvalidosDevuelven400SinGuardarRutinas() throws Exception {
        long antes = rutinas.count();
        for (String nombre : new String[] {null, "", "   ", "a".repeat(31)}) {
            mvc.perform(post("/api/rutina/create")
                            .header("Authorization", "Bearer " + token)
                            .contentType("application/json")
                            .content(json.writeValueAsString(Collections.singletonMap("nombre", nombre))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.nombre").exists());
        }
        mvc.perform(post("/api/rutina/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        assertThat(rutinas.count()).isEqualTo(antes);
    }

    @Test
    void nombreDeTreintaCaracteresEsValido() throws Exception {
        String nombre = "a".repeat(30);
        mvc.perform(post("/api/rutina/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(json.writeValueAsString(Map.of("nombre", nombre))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value(nombre));
    }

    @Test
    void nombreSeGuardaSinEspaciosEnLosExtremos() throws Exception {
        mvc.perform(post("/api/rutina/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"nombre\":\"  Torso  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Torso"));
    }

    @Test
    void jsonMalformadoDevuelve400SinGuardar() throws Exception {
        long antes = rutinas.count();
        mvc.perform(post("/api/rutina/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{mal"))
                .andExpect(status().isBadRequest());
        assertThat(rutinas.count()).isEqualTo(antes);
    }

    @Test
    void crearYListarRequierenSesion() throws Exception {
        long antes = rutinas.count();
        mvc.perform(get("/api/rutina")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/rutina/create")
                        .contentType("application/json").content("{\"nombre\":\"Torso\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(rutinas.count()).isEqualTo(antes);
    }

    @Test
    void tokenInventadoNoPermiteCrearNiListar() throws Exception {
        long antes = rutinas.count();
        String authorization = "Bearer " + "x".repeat(43);
        mvc.perform(get("/api/rutina").header("Authorization", authorization))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/rutina/create").header("Authorization", authorization)
                        .contentType("application/json").content("{\"nombre\":\"Torso\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(rutinas.count()).isEqualTo(antes);
    }

    @Test
    void cerrarSesionImpideReutilizarElTokenEnRutinas() throws Exception {
        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/rutina").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/rutina/create").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"nombre\":\"Torso\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rutinaCreadaApareceEnElListadoConElMismoId() throws Exception {
        var respuesta = mvc.perform(post("/api/rutina/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"nombre\":\"Nueva\"}"))
                .andExpect(status().isCreated()).andReturn();
        int id = json.readTree(respuesta.getResponse().getContentAsString()).get("id").asInt();
        mvc.perform(get("/api/rutina").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].nombre").value("Nueva"));
    }

    @Test
    void editarNombreActualizaLaRutinaPropia() throws Exception {
        Rutina rutina = rutinas.saveAndFlush(new Rutina("Nombre anterior", propietario));

        mvc.perform(patch("/api/rutina/{id}", rutina.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"nombre\":\"Nombre nuevo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(rutina.getId()))
                .andExpect(jsonPath("$.nombre").value("Nombre nuevo"));

        assertThat(rutinas.findById(rutina.getId()).orElseThrow().getNombre())
                .isEqualTo("Nombre nuevo");
    }

    @Test
    void editarNombreValidaElContenido() throws Exception {
        Rutina rutina = rutinas.saveAndFlush(new Rutina("Original", propietario));

        mvc.perform(patch("/api/rutina/{id}", rutina.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"nombre\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nombre").exists());

        assertThat(rutinas.findById(rutina.getId()).orElseThrow().getNombre())
                .isEqualTo("Original");
    }

    @Test
    void duplicarCreaOtraRutinaConSusDiasYReferenciaAlOrigen() throws Exception {
        Rutina original = new Rutina("Rutina base", propietario);
        Ejercicio ejercicio = new Ejercicio("Press banca", "Fuerza", new String[] {"Pectoral"},
                new String[0], "Barra", TipoMedicion.REPS_CARGA, Visibilidad.PUBLICO, null);
        entityManager.persist(ejercicio);
        original.anadirDia("Día 1").anadirEjercicio(ejercicio, 3, 8, 12);
        rutinas.saveAndFlush(original);

        var respuesta = mvc.perform(post("/api/rutina/{id}/duplicar", original.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Rutina base"))
                .andReturn();

        int copiaId = json.readTree(respuesta.getResponse().getContentAsString()).get("id").asInt();
        entityManager.flush();
        entityManager.clear();
        Rutina copia = rutinas.findById(copiaId).orElseThrow();
        assertThat(copia.getId()).isNotEqualTo(original.getId());
        assertThat(copia.getRutinaOrigen().getId()).isEqualTo(original.getId());
        assertThat(copia.getDias()).hasSize(1);
        assertThat(copia.getDias().getFirst().getNombre()).isEqualTo("Día 1");
        var ejercicioCopiado = copia.getDias().getFirst().getEjercicios().getFirst();
        assertThat(ejercicioCopiado.getSeriesObjetivo()).isEqualTo(3);
        assertThat(ejercicioCopiado.getRepeticionesMin()).isEqualTo(8);
        assertThat(ejercicioCopiado.getRepeticionesMax()).isEqualTo(12);
        assertThat(ejercicioCopiado.getId()).isNotEqualTo(original.getDias().getFirst().getEjercicios().getFirst().getId());
    }

    @Test
    void eliminarEsLogicoYDesapareceDelListado() throws Exception {
        Rutina rutina = rutinas.saveAndFlush(new Rutina("Para eliminar", propietario));

        mvc.perform(delete("/api/rutina/{id}", rutina.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(rutinas.findById(rutina.getId()).orElseThrow().isEliminada()).isTrue();
        mvc.perform(get("/api/rutina").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void otroUsuarioNoPuedeEditarDuplicarNiEliminarLaRutina() throws Exception {
        Rutina rutina = rutinas.saveAndFlush(new Rutina("Privada", propietario));
        String tokenOtro = login(otro);

        mvc.perform(patch("/api/rutina/{id}", rutina.getId())
                        .header("Authorization", "Bearer " + tokenOtro)
                        .contentType("application/json")
                        .content("{\"nombre\":\"Intrusión\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/rutina/{id}/duplicar", rutina.getId())
                        .header("Authorization", "Bearer " + tokenOtro))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/rutina/{id}", rutina.getId())
                        .header("Authorization", "Bearer " + tokenOtro))
                .andExpect(status().isNotFound());

        Rutina sinCambios = rutinas.findById(rutina.getId()).orElseThrow();
        assertThat(sinCambios.getNombre()).isEqualTo("Privada");
        assertThat(sinCambios.isEliminada()).isFalse();
        assertThat(rutinas.count()).isEqualTo(1);
    }

    @Test
    void unaRutinaEliminadaNoSePuedeEditarDuplicarNiEliminarOtraVez() throws Exception {
        Rutina rutina = new Rutina("Eliminada", propietario);
        rutina.eliminar();
        rutinas.saveAndFlush(rutina);

        mvc.perform(patch("/api/rutina/{id}", rutina.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"nombre\":\"Nuevo\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/rutina/{id}/duplicar", rutina.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/rutina/{id}", rutina.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void usuarioInexistenteEnElServiceProduce404SinCrearRutina() {
        UsuarioRespuesta inexistente = new UsuarioRespuesta(Long.MAX_VALUE, "Ausente",
                "ausente@example.com", Set.of("CLIENTE"));
        long antes = rutinas.count();
        var crear = assertThrows(ResponseStatusException.class,
                () -> service.crearRutina(new CrearRutinaDTO("Prueba"), inexistente));
        var listar = assertThrows(ResponseStatusException.class, () -> service.listarRutinas(inexistente));
        assertThat(crear.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(listar.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(rutinas.count()).isEqualTo(antes);
    }

    @Test
    void consultarDetalleDevuelveDiasYEntradasConSusDosIds() throws Exception {
        Ejercicio ejercicio = new Ejercicio("Press banca", "Fuerza", new String[] {"Pectoral"},
                new String[0], "Barra", TipoMedicion.REPS_CARGA, Visibilidad.PUBLICO, null);
        entityManager.persist(ejercicio);
        Rutina rutina = new Rutina("Torso", propietario);
        DiaRutina primero = rutina.anadirDia("Dia 1");
        EjercicioRutina entrada = primero.anadirEjercicio(ejercicio, 3, 8, 12);
        DiaRutina segundo = rutina.anadirDia("Dia 2");
        EjercicioRutina otraEntrada = segundo.anadirEjercicio(ejercicio, 4, 10, 10);
        rutinas.saveAndFlush(rutina);
        entityManager.clear();

        mvc.perform(get("/api/rutina/{id}", rutina.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(rutina.getId()))
                .andExpect(jsonPath("$.nombre").value("Torso"))
                .andExpect(jsonPath("$.dias.length()").value(2))
                .andExpect(jsonPath("$.dias[0].id").value(primero.getId()))
                .andExpect(jsonPath("$.dias[0].nombre").value("Dia 1"))
                .andExpect(jsonPath("$.dias[0].orden").value(1))
                .andExpect(jsonPath("$.dias[0].ejercicios.length()").value(1))
                .andExpect(jsonPath("$.dias[0].ejercicios[0].id").value(entrada.getId()))
                .andExpect(jsonPath("$.dias[0].ejercicios[0].ejercicioId").value(ejercicio.getId()))
                .andExpect(jsonPath("$.dias[0].ejercicios[0].nombre").value("Press banca"))
                .andExpect(jsonPath("$.dias[0].ejercicios[0].seriesObjetivo").value(3))
                .andExpect(jsonPath("$.dias[0].ejercicios[0].repeticionesMin").value(8))
                .andExpect(jsonPath("$.dias[0].ejercicios[0].repeticionesMax").value(12))
                .andExpect(jsonPath("$.dias[1].orden").value(2))
                .andExpect(jsonPath("$.dias[1].ejercicios[0].id").value(otraEntrada.getId()))
                .andExpect(jsonPath("$.dias[1].ejercicios[0].ejercicioId").value(ejercicio.getId()))
                .andExpect(jsonPath("$.usuario").doesNotExist());
    }

    @Test
    void consultarDetalleAdmiteRutinasYDiasVacios() throws Exception {
        Rutina vacia = rutinas.saveAndFlush(new Rutina("Vacia", propietario));
        mvc.perform(get("/api/rutina/{id}", vacia.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dias").isEmpty());

        vacia.anadirDia("Sin ejercicios");
        rutinas.flush();
        entityManager.clear();
        mvc.perform(get("/api/rutina/{id}", vacia.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dias[0].ejercicios").isEmpty());
    }

    @Test
    void consultarDetalleRequiereSesionYNoExponeRutinasAjenasEliminadasOInexistentes() throws Exception {
        Rutina propia = rutinas.saveAndFlush(new Rutina("Propia", propietario));
        Rutina ajena = rutinas.saveAndFlush(new Rutina("Ajena", otro));
        Rutina eliminada = new Rutina("Eliminada", propietario);
        eliminada.eliminar();
        rutinas.saveAndFlush(eliminada);

        mvc.perform(get("/api/rutina/{id}", propia.getId()))
                .andExpect(status().isUnauthorized());
        for (int id : new int[] {ajena.getId(), eliminada.getId(), Integer.MAX_VALUE}) {
            mvc.perform(get("/api/rutina/{id}", id).header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void guardaRutinaConAproximacionesTecnicaYEsfuerzoYConservaLosIdsAlReordenar() throws Exception {
        Ejercicio ejercicio = ejercicioPublico("Press");
        Rutina rutina = rutinas.saveAndFlush(new Rutina("Base", propietario));
        ConfigurarEjercicioDTO datos = new ConfigurarEjercicioDTO(ejercicio.getId(), 3, 8, 12, 120,
                new EsfuerzoDTO(2, null), "Bajada controlada", List.of(new AproximacionDTO(10, 20.0, 60)));
        var respuesta = mvc.perform(put("/api/rutina/{id}", rutina.getId())
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(json.writeValueAsString(new GuardarRutinaDTO(rutina.getVersion(), "Editada", List.of(
                                new DiaEntradaDTO(null, "Primero", List.of(new EntradaRutinaDTO(null, datos))),
                                new DiaEntradaDTO(null, "Segundo", List.of()))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dias[0].ejercicios[0].esfuerzoObjetivo.rir").value(2))
                .andExpect(jsonPath("$.dias[0].ejercicios[0].descansoSeg").value(120))
                .andExpect(jsonPath("$.dias[0].ejercicios[0].tecnicaOriginal").value("Técnica original"))
                .andExpect(jsonPath("$.dias[0].ejercicios[0].tecnicaPersonalizada").value("Bajada controlada"))
                .andExpect(jsonPath("$.dias[0].ejercicios[0].aproximaciones[0].carga").value(20.0))
                .andReturn();
        var cuerpo = json.readTree(respuesta.getResponse().getContentAsString());
        int primero = cuerpo.get("dias").get(0).get("id").asInt();
        int segundo = cuerpo.get("dias").get(1).get("id").asInt();
        int entrada = cuerpo.get("dias").get(0).get("ejercicios").get(0).get("id").asInt();
        mvc.perform(put("/api/rutina/{id}", rutina.getId())
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(json.writeValueAsString(new GuardarRutinaDTO(cuerpo.get("version").asLong(), "Editada", List.of(
                                new DiaEntradaDTO(segundo, "Segundo", List.of()),
                                new DiaEntradaDTO(primero, "Primero", List.of(new EntradaRutinaDTO(entrada, datos))))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dias[0].id").value(segundo))
                .andExpect(jsonPath("$.dias[1].ejercicios[0].id").value(entrada));
        entityManager.clear();
        mvc.perform(get("/api/rutina/{id}", rutina.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dias[0].id").value(segundo));
    }

    @Test
    void iniciaRecuperaYModificaUnBorradorSinCambiarLaRutinaYFinaliza() throws Exception {
        Ejercicio original = ejercicioPublico("Press");
        Ejercicio sustituto = ejercicioPublico("Mancuernas");
        Rutina rutina = new Rutina("Base", propietario);
        DiaRutina dia = rutina.anadirDia("Torso");
        dia.anadirEjercicio(original, 3, 8, 12).configurar(120, new Esfuerzo(2, null), "Técnica del día",
                List.of(new Aproximacion(10, 20.0, 60)));
        rutinas.saveAndFlush(rutina);
        var inicio = mvc.perform(post("/api/sesiones").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(json.writeValueAsString(new IniciarSesionDTO(rutina.getId(), dia.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.finalizada").value(false))
                .andExpect(jsonPath("$.ejercicios[0].series").isEmpty())
                .andExpect(jsonPath("$.ejercicios[0].plan.tecnicaOriginal").value("Técnica original"))
                .andExpect(jsonPath("$.ejercicios[0].aproximaciones[0].repeticiones").value(10)).andReturn();
        var cuerpo = json.readTree(inicio.getResponse().getContentAsString());
        int id = cuerpo.get("id").asInt();
        int registroId = cuerpo.get("ejercicios").get(0).get("id").asInt();
        var plan = new PlanDTO(4, 6, 10, 90, new EsfuerzoDTO(null, 8.5), "Técnica original", "Adaptada");
        var valoracion = new ValoracionDTO(8, 9, 7, 1, 8, "Máquina ocupada, uso mancuernas");
        var series = List.of(new SerieDTO(TipoSerie.APROXIMACION, 10, 10, 0, 0, null, 45, null),
                new SerieDTO(TipoSerie.EFECTIVA, 8, 25, 0, 0, new EsfuerzoDTO(null, 8.5), 90, null));
        var registros = List.of(new RegistroEntradaDTO(registroId, sustituto.getId(), plan,
                List.of(new AproximacionDTO(10, 10.0, 45)), valoracion, series));
        var guardado = mvc.perform(put("/api/sesiones/{id}", id).header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(json.writeValueAsString(new GuardarSesionDTO(cuerpo.get("version").asLong(), 20, false, registros))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.finalizada").value(false)).andReturn();
        entityManager.clear();
        mvc.perform(get("/api/sesiones/{id}", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ejercicios[0].ejercicioId").value(sustituto.getId()))
                .andExpect(jsonPath("$.ejercicios[0].valoracion.comodidad").value(8))
                .andExpect(jsonPath("$.ejercicios[0].valoracion.comentariosUsuario").value("Máquina ocupada, uso mancuernas"))
                .andExpect(jsonPath("$.ejercicios[0].series[0].tipo").value("APROXIMACION"))
                .andExpect(jsonPath("$.ejercicios[0].series[1].esfuerzo.rpe").value(8.5));
        mvc.perform(get("/api/rutina/{id}", rutina.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dias[0].ejercicios[0].ejercicioId").value(original.getId()))
                .andExpect(jsonPath("$.dias[0].ejercicios[0].seriesObjetivo").value(3));
        mvc.perform(get("/api/sesiones/en-curso").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        long version = json.readTree(guardado.getResponse().getContentAsString()).get("version").asLong();
        mvc.perform(put("/api/sesiones/{id}", id).header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(json.writeValueAsString(new GuardarSesionDTO(version, 30, true, registros))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.finalizada").value(true));
        mvc.perform(get("/api/sesiones/en-curso").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void valoracionesYEsfuerzoInvalidosNoSeAceptanYLosPrivadosAjenosNoSeListan() throws Exception {
        var privado = new Ejercicio("Privado", "Fuerza", new String[0], new String[0], "Barra",
                TipoMedicion.REPS_CARGA, Visibilidad.PRIVADO, otro);
        entityManager.persist(privado);
        mvc.perform(get("/api/ejercicios").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new ValoracionEjercicio(0, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new ValoracionEjercicio(null, null, null, 11, null, null));
        assertThrows(IllegalArgumentException.class, () -> new Esfuerzo(2, 8.0));
        assertThrows(IllegalArgumentException.class, () -> new Esfuerzo(null, Double.NaN));
    }

    @Test
    void sesionNoAdmiteAccesoAjenoNiVersionObsoleta() throws Exception {
        Ejercicio e = ejercicioPublico("Press");
        Rutina rutina = new Rutina("Base", propietario);
        DiaRutina dia = rutina.anadirDia("Torso");
        dia.anadirEjercicio(e, 3, 8, 12);
        rutinas.saveAndFlush(rutina);
        var inicio = mvc.perform(post("/api/sesiones").header("Authorization", "Bearer " + token)
                .contentType("application/json").content(json.writeValueAsString(new IniciarSesionDTO(rutina.getId(), dia.getId()))))
                .andExpect(status().isCreated()).andReturn();
        int id = json.readTree(inicio.getResponse().getContentAsString()).get("id").asInt();
        mvc.perform(get("/api/sesiones/{id}", id)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/sesiones/{id}", id).header("Authorization", "Bearer " + login(otro)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/sesiones/{id}", id).header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(json.writeValueAsString(new GuardarSesionDTO(100, 0, false, List.of()))))
                .andExpect(status().isConflict());
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void unErrorTrasModificarLaRutinaRevierteNombreDiasYVersionEnLaBase() throws Exception {
        Rutina rutina = new Rutina("Original", propietario);
        rutina.anadirDia("Conservar");
        rutinas.saveAndFlush(rutina);
        // El ejercicio se guarda en su propia transacción; esta prueba no envuelve las peticiones en una transacción común.
        var plantilla = new org.springframework.transaction.support.TransactionTemplate(
                new org.springframework.orm.jpa.JpaTransactionManager(entityManager.getEntityManagerFactory()));
        int ejercicioId = plantilla.execute(estado -> ejercicioPublico("Press para rollback").getId());
        ConfigurarEjercicioDTO invalido = new ConfigurarEjercicioDTO(ejercicioId, 3, 8, 12, 90,
                new EsfuerzoDTO(2, 8.0), null, List.of());
        try {
        mvc.perform(put("/api/rutina/{id}", rutina.getId()).header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(json.writeValueAsString(new GuardarRutinaDTO(
                                rutina.getVersion(), "No debe guardarse", List.of(new DiaEntradaDTO(null, "Nuevo",
                                List.of(new EntradaRutinaDTO(null, invalido))))))))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/rutina/{id}", rutina.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Original"))
                .andExpect(jsonPath("$.version").value((int) rutina.getVersion()))
                .andExpect(jsonPath("$.dias.length()").value(1))
                .andExpect(jsonPath("$.dias[0].nombre").value("Conservar"));
        } finally {
            rutinas.deleteById(rutina.getId());
            plantilla.executeWithoutResult(estado -> entityManager.remove(entityManager.find(Ejercicio.class, ejercicioId)));
        }
    }

    @Test
    void alternativasSeGuardanDuplicanYSeCopianALaSesionSinCambiarLaRutinaAlElegirUna() throws Exception {
        Ejercicio principal = ejercicioPublico("Press banca");
        Ejercicio primera = new Ejercicio("Press mancuernas", "Fuerza", new String[0], new String[0], "Mancuernas",
                TipoMedicion.REPS_CARGA, Visibilidad.PRIVADO, propietario);
        entityManager.persist(primera);
        Ejercicio segunda = ejercicioPublico("Press máquina");
        Rutina rutina = rutinas.saveAndFlush(new Rutina("Con alternativas", propietario));
        var datos = new ConfigurarEjercicioDTO(principal.getId(), 3, 8, 12, 90, null, null,
                List.of(), List.of(primera.getId(), segunda.getId()));
        var guardado = mvc.perform(put("/api/rutina/{id}", rutina.getId())
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(json.writeValueAsString(new GuardarRutinaDTO(rutina.getVersion(), rutina.getNombre(),
                                List.of(new DiaEntradaDTO(null, "Torso", List.of(new EntradaRutinaDTO(null, datos))))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dias[0].ejercicios[0].alternativas[0].ejercicioId").value(primera.getId()))
                .andExpect(jsonPath("$.dias[0].ejercicios[0].alternativas[1].nombre").value("Press máquina"))
                .andReturn();
        var detalle = json.readTree(guardado.getResponse().getContentAsString());
        int diaId = detalle.get("dias").get(0).get("id").asInt();
        int entradaId = detalle.get("dias").get(0).get("ejercicios").get(0).get("id").asInt();
        var inicio = mvc.perform(post("/api/sesiones").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(json.writeValueAsString(new IniciarSesionDTO(rutina.getId(), diaId))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.ejercicios[0].alternativas.length()").value(2)).andReturn();
        var sesion = json.readTree(inicio.getResponse().getContentAsString());
        int sesionId = sesion.get("id").asInt();
        int registroId = sesion.get("ejercicios").get(0).get("id").asInt();

        var duplicada = mvc.perform(post("/api/rutina/{id}/duplicar", rutina.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()).andReturn();
        int copiaId = json.readTree(duplicada.getResponse().getContentAsString()).get("id").asInt();
        entityManager.flush();
        entityManager.clear();
        mvc.perform(get("/api/rutina/{id}", copiaId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dias[0].ejercicios[0].alternativas.length()").value(2));

        // Reordenar opciones y volver a leerlas verifica la persistencia de la lista.
        var invertidas = new ConfigurarEjercicioDTO(principal.getId(), 3, 8, 12, 90, null, null,
                List.of(), List.of(segunda.getId(), primera.getId()));
        var reordenada = mvc.perform(put("/api/rutina/{id}", rutina.getId()).header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(json.writeValueAsString(new GuardarRutinaDTO(detalle.get("version").asLong(), rutina.getNombre(),
                                List.of(new DiaEntradaDTO(diaId, "Torso", List.of(new EntradaRutinaDTO(entradaId, invertidas))))))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dias[0].ejercicios[0].alternativas[0].ejercicioId").value(segunda.getId()))
                .andReturn();
        var actual = json.readTree(reordenada.getResponse().getContentAsString());
        var sinAlternativas = new ConfigurarEjercicioDTO(principal.getId(), 3, 8, 12, 90, null, null, List.of());
        mvc.perform(put("/api/rutina/{id}", rutina.getId()).header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(json.writeValueAsString(new GuardarRutinaDTO(actual.get("version").asLong(), rutina.getNombre(),
                                List.of(new DiaEntradaDTO(diaId, "Torso", List.of(new EntradaRutinaDTO(entradaId, sinAlternativas))))))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dias[0].ejercicios[0].alternativas").isEmpty());

        mvc.perform(put("/api/sesiones/{id}", sesionId).header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(json.writeValueAsString(new GuardarSesionDTO(sesion.get("version").asLong(), 0, false,
                                List.of(new RegistroEntradaDTO(registroId, primera.getId(), null, List.of(), null, List.of()))))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ejercicios[0].ejercicioId").value(primera.getId()));
        entityManager.clear();
        mvc.perform(get("/api/sesiones/{id}", sesionId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ejercicios[0].alternativas[0].ejercicioId").value(primera.getId()))
                .andExpect(jsonPath("$.ejercicios[0].ejercicioPropuesto.ejercicioId").value(principal.getId()))
                .andExpect(jsonPath("$.ejercicios[0].alternativas.length()").value(2));
        mvc.perform(get("/api/rutina/{id}", rutina.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dias[0].ejercicios[0].ejercicioId").value(principal.getId()))
                .andExpect(jsonPath("$.dias[0].ejercicios[0].alternativas").isEmpty());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"principal", "repetida", "privada", "inexistente"})
    void rechazaAlternativasInvalidasONoAccesibles(String caso) throws Exception {
        Ejercicio principal = ejercicioPublico("Principal");
        Ejercicio alternativa = ejercicioPublico("Alternativa");
        Ejercicio privado = new Ejercicio("Privado ajeno", "Fuerza", new String[0], new String[0], "Barra",
                TipoMedicion.REPS_CARGA, Visibilidad.PRIVADO, otro);
        entityManager.persist(privado);
        Rutina rutina = rutinas.saveAndFlush(new Rutina("Prueba", propietario));
        List<Integer> ids = switch (caso) {
            case "principal" -> List.of(principal.getId());
            case "repetida" -> List.of(alternativa.getId(), alternativa.getId());
            case "privada" -> List.of(privado.getId());
            default -> List.of(Integer.MAX_VALUE);
        };
        var datos = new ConfigurarEjercicioDTO(principal.getId(), 3, 8, 12, null, null, null, List.of(), ids);
        mvc.perform(put("/api/rutina/{id}", rutina.getId()).header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(json.writeValueAsString(new GuardarRutinaDTO(rutina.getVersion(), "Prueba",
                                List.of(new DiaEntradaDTO(null, "Torso", List.of(new EntradaRutinaDTO(null, datos))))))))
                .andExpect(status().is(caso.equals("privada") || caso.equals("inexistente") ? 404 : 400));
    }

    private Ejercicio ejercicioPublico(String nombre) {
        Ejercicio e = new Ejercicio(nombre, "Fuerza", new String[0], new String[0], "Barra", TipoMedicion.REPS_CARGA, Visibilidad.PUBLICO, null);
        e.describirTecnica("Técnica original");
        entityManager.persist(e);
        return e;
    }

    private Usuario crearUsuario(String email, String hash) {
        Usuario usuario = new Usuario("Prueba", email, hash,
                Set.of(roles.findById("CLIENTE").orElseThrow()));
        usuario.verificarEmail();
        return usuarios.saveAndFlush(usuario);
    }

    private String login(Usuario usuario) throws Exception {
        var respuesta = mvc.perform(post("/api/auth/login").contentType("application/json")
                        .content(json.writeValueAsString(Map.of("email", usuario.getEmail(),
                                "contrasena", "ClaveDePrueba123"))))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(respuesta.getResponse().getContentAsString()).get("accessToken").asString();
    }
}
