package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.service;

import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.*;
import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.dto.EntrenamientoDtos.*;
import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.repository.*;
import dam.com.tfg.mygymbro.mygymbro_api.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.util.*;

/**
 * Gestiona entrenamientos propios iniciados, borradores y finalización.
 * La planificación se copia al iniciar; editar los registros de la sesión no escribe en la rutina.
 */
@Service
public class SesionService {
    private final SesionEntrenamientoRepository sesiones;
    private final RutinaRepository rutinas;
    private final UsuarioRepository usuarios;
    private final AccesoEjercicio ejercicios;
    private final Clock clock;
    private final EntityManager em;

    public SesionService(SesionEntrenamientoRepository sesiones, RutinaRepository rutinas,
                        UsuarioRepository usuarios, AccesoEjercicio ejercicios, Clock clock, EntityManager em) {
        this.sesiones = sesiones;
        this.rutinas = rutinas;
        this.usuarios = usuarios;
        this.ejercicios = ejercicios;
        this.clock = clock;
        this.em = em;
    }

    /**
     * Inicia una sesión copiando un día que pertenece a una rutina propia activa.
     * Comprueba el acceso a los ejercicios principales y copia objetivos, aproximaciones,
     * técnica, alternativas y referencia al principal propuesto. No genera resultados de series.
     *
     * @param datos IDs de la rutina y del día
     * @param usuarioId ID autenticado del propietario de la nueva sesión
     * @return borrador persistido, sin finalizar, con ID y versión
     * @throws ResponseStatusException con estado 404 si la rutina, el día o un ejercicio no son accesibles
     */
    @Transactional
    public SesionDTO iniciar(IniciarSesionDTO datos, Long usuarioId) {
        Rutina rutina = rutinas.findByIdAndUsuarioIdAndEliminadaFalse(datos.rutinaId(), usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La rutina no existe"));
        DiaRutina dia = rutina.getDias().stream().filter(d -> d.getId() == datos.diaId()).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El día no existe en esta rutina"));
        dia.getEjercicios().forEach(e -> ejercicios.obtener(e.getEjercicio().getId(), usuarioId));
        SesionEntrenamiento sesion = new SesionEntrenamiento(usuarios.findById(usuarioId).orElseThrow(), rutina,
                clock.instant(), 0, 0);
        sesion.copiarDia(dia);
        sesiones.saveAndFlush(sesion);
        return convertir(sesion);
    }

    /**
     * Consulta el detalle de una sesión propia, esté en curso o finalizada.
     *
     * @param id ID de la sesión
     * @param usuarioId ID del usuario autenticado
     * @return copia de la planificación y resultados registrados en forma de DTO
     * @throws ResponseStatusException con estado 404 si la sesión no es propia o no existe
     */
    @Transactional(readOnly = true)
    public SesionDTO consultar(int id, Long usuarioId) { return convertir(obtener(id, usuarioId)); }

    /**
     * Recupera los borradores propios para continuar un entrenamiento guardado.
     *
     * @param usuarioId ID del usuario autenticado
     * @return sesiones sin finalizar ordenadas por fecha descendente e ID descendente
     */
    @Transactional(readOnly = true)
    public List<SesionDTO> enCurso(Long usuarioId) {
        return sesiones.findByUsuarioIdAndFinalizadaFalseOrderByFechaDescIdDesc(usuarioId).stream().map(this::convertir).toList();
    }

    /**
     * Guarda una edición completa de una sesión propia en curso, sin modificar la rutina.
     * Crea registros con ID nulo, actualiza los existentes y elimina los omitidos; comprueba
     * su pertenencia, acceso al ejercicio, mediciones, esfuerzo y valoraciones.
     * Permite sustituir el principal por una alternativa u otro ejercicio accesible del catálogo.
     * El orden recibido fija posiciones y números de serie. Las propuestas originales se conservan.
     * Si se solicita finalizar, exige al menos un ejercicio y una serie por ejercicio;
     * con borrador se permiten listas vacías. Un error revierte toda la transacción.
     *
     * @param id ID de la sesión
     * @param datos versión, duración, estado deseado y registros completos
     * @param usuarioId ID del usuario autenticado
     * @return sesión persistida con versión y estado actualizados
     * @throws ResponseStatusException con estado 404 si un recurso no es accesible, o 409 por versión antigua
     * @throws IllegalArgumentException si los registros, mediciones o valoraciones no son válidos
     * @throws IllegalStateException si ya estaba finalizada o no se cumplen las condiciones para finalizar
     */
    @Transactional
    public SesionDTO guardar(int id, GuardarSesionDTO datos, Long usuarioId) {
        SesionEntrenamiento sesion = obtener(id, usuarioId);
        if (sesion.getVersion() != datos.version()) throw new ResponseStatusException(HttpStatus.CONFLICT, "La sesión ha cambiado; vuelve a cargarla");
        sesion.guardarBorrador(datos.duracionMin());
        Map<Integer, RegistroEjercicio> anteriores = new HashMap<>();
        sesion.getRegistros().forEach(r -> anteriores.put(r.getId(), r));
        Set<Integer> usados = new HashSet<>();
        for (var r : datos.ejercicios()) {
            if (r.id() != null && (!anteriores.containsKey(r.id()) || !usados.add(r.id()))) {
                throw new IllegalArgumentException("El registro está repetido o no pertenece a la sesión");
            }
        }
        sesion.conservarRegistros(usados);
        int orden = 0;
        for (var entrada : datos.ejercicios()) {
            Ejercicio ejercicio = ejercicios.obtener(entrada.ejercicioId(), usuarioId);
            RegistroEjercicio registro = entrada.id() == null ? sesion.anadirRegistro(ejercicio) : anteriores.get(entrada.id());
            registro.sustituirEjercicio(ejercicio);
            registro.ordenar(++orden);
            registro.configurar(entrada.plan() == null ? null : entrada.plan().dominio(),
                    entrada.aproximaciones().stream().map(AproximacionDTO::dominio).toList(),
                    entrada.valoracion() == null ? null : entrada.valoracion().dominio());
            List<SerieRegistrada> series = new ArrayList<>();
            for (var serie : entrada.series()) series.add(serie.dominio(series.size() + 1));
            registro.sustituirSeries(series);
        }
        if (datos.finalizar()) sesion.finalizar();
        em.flush();
        em.refresh(sesion);
        return convertir(sesion);
    }

    /** Busca ID y propietario juntos; no permite usar un ID conocido para acceder a sesiones ajenas. */
    private SesionEntrenamiento obtener(int id, Long usuarioId) {
        return sesiones.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La sesión no existe"));
    }

    /**
     * Prepara el detalle HTTP sin serializar entidades JPA y filtra las propuestas no accesibles.
     * Conserva la distinción entre el ejercicio actualmente realizado y el principal propuesto al iniciar.
     */
    private SesionDTO convertir(SesionEntrenamiento s) {
        return new SesionDTO(s.getId(), s.getVersion(), s.getRutina() == null ? null : s.getRutina().getId(),
                s.getDiaRutinaOrigenId(), s.getNombreDiaOrigen(), s.getFecha(), s.getDuracionMin(), s.isFinalizada(),
                s.getRegistros().stream().map(r -> new RegistroDTO(r.getId(), r.getEjercicio().getId(),
                        r.getEjercicio().getNombre(), PlanDTO.de(r.getPlan()),
                        r.getAproximaciones().stream().map(AproximacionDTO::de).toList(),
                        ValoracionDTO.de(r.getValoracion()), r.getSeries().stream().map(SerieDTO::de).toList(),
                        r.getAlternativas().stream().filter(e -> ejercicios.esAccesible(e, s.getUsuario().getId()))
                                .map(AlternativaDTO::de).toList(),
                        r.getEjercicioPropuesto() != null && ejercicios.esAccesible(r.getEjercicioPropuesto(), s.getUsuario().getId())
                                ? AlternativaDTO.de(r.getEjercicioPropuesto()) : null)).toList());
    }
}
