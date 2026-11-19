package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.service;

import dam.com.tfg.mygymbro.mygymbro_api.autenticacion.AuthDtos;
import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.DiaRutina;
import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.EjercicioRutina;
import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.Rutina;
import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.dto.*;
import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.repository.RutinaRepository;
import dam.com.tfg.mygymbro.mygymbro_api.usuario.Usuario;
import dam.com.tfg.mygymbro.mygymbro_api.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona la planificación de rutinas propias: cabecera, días y ejercicios.
 * Comprueba el propietario antes de acceder a una rutina y construye los DTO para HTTP.
 * Los cambios de entrenamientos realizados corresponden a {@link SesionService}.
 */
@Service
public class RutinaService {
    RutinaRepository rutinaRepository;
    UsuarioRepository usuarioRepository;
    private final AccesoEjercicio accesoEjercicio;
    private final EntityManager entityManager;

    public RutinaService(RutinaRepository rutinaRepository, UsuarioRepository usuarioRepository,
                         AccesoEjercicio accesoEjercicio, EntityManager entityManager) {
        this.rutinaRepository = rutinaRepository;
        this.usuarioRepository = usuarioRepository;
        this.accesoEjercicio = accesoEjercicio;
        this.entityManager = entityManager;
    }

    /**
     * Crea y guarda una rutina vacía cuyo propietario es el usuario autenticado.
     *
     * @param request          nombre de la nueva rutina
     * @param usuarioRespuesta identidad autenticada; no se acepta un propietario elegido en el cuerpo
     * @return ID y nombre guardados
     * @throws ResponseStatusException con estado 404 si el usuario no existe
     */
    @Transactional
    public RutinaDTO crearRutina(CrearRutinaDTO request, AuthDtos.UsuarioRespuesta usuarioRespuesta) {

        Usuario usuario = obtenerUsuarioPorId(usuarioRespuesta.id());
        Rutina rutina = new Rutina(request.nombre(), usuario);
        Rutina guardada = rutinaRepository.save(rutina);

        return new RutinaDTO(guardada.getNombre(), guardada.getId());
    }

    /**
     * Resuelve el usuario persistido a partir del ID y produce 404 si no existe.
     */
    private Usuario obtenerUsuarioPorId(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se ha encontrado el usuario"));
    }

    /**
     * Consulta las cabeceras de las rutinas propias activas para el listado inicial.
     *
     * @param usuarioRespuesta identidad autenticada
     * @return IDs y nombres; no carga el árbol de días y ejercicios en la respuesta
     */
    @Transactional(readOnly = true)
    public List<RutinaDTO> listarRutinas(AuthDtos.UsuarioRespuesta usuarioRespuesta) {
        Usuario usuario = obtenerUsuarioPorId(usuarioRespuesta.id());

        return rutinaRepository
                .findAllByUsuarioIdAndEliminadaFalse(usuario.getId());
    }

    /**
     * Cambia solo el nombre de una rutina propia activa dentro de una transacción.
     *
     * @param id               ID de la rutina
     * @param datos            nuevo nombre
     * @param usuarioRespuesta identidad autenticada usada para comprobar la propiedad
     * @return cabecera actualizada
     * @throws ResponseStatusException con estado 404 si la rutina no es accesible
     */
    @Transactional
    public RutinaDTO editarNombre(int id,
                                  EditarRutinaDTO datos,
                                  AuthDtos.UsuarioRespuesta usuarioRespuesta) {
        Rutina rutina = obtenerRutinaActivaDelUsuario(id, usuarioRespuesta.id());
        rutina.editar(datos.nombre());
        return convertirDTO(rutina);
    }

    /**
     * Crea una nueva rutina copiando los días y las entradas de una rutina propia activa.
     * Conserva objetivos, técnica personalizada, aproximaciones y alternativas, y referencia al origen.
     * Las definiciones del catálogo se comparten; los entrenamientos realizados no se copian.
     *
     * @param id               ID de la rutina de origen
     * @param usuarioRespuesta usuario autenticado, propietario del origen y de la copia
     * @return ID nuevo y nombre de la copia
     * @throws ResponseStatusException con estado 404 si el origen no es accesible
     */
    @Transactional
    public RutinaDTO duplicarRutina(int id, AuthDtos.UsuarioRespuesta usuarioRespuesta) {
        Rutina original = obtenerRutinaActivaDelUsuario(id, usuarioRespuesta.id());
        Rutina copia = rutinaRepository.save(original.duplicar());
        return convertirDTO(copia);
    }

    /**
     * Oculta una rutina propia activa mediante su marca de eliminación lógica.
     * No borra físicamente su fila ni los entrenamientos que la referencian.
     *
     * @param id               ID de la rutina
     * @param usuarioRespuesta identidad autenticada
     * @throws ResponseStatusException con estado 404 si la rutina es ajena, ya eliminada o inexistente
     */
    @Transactional
    public void eliminarRutina(int id, AuthDtos.UsuarioRespuesta usuarioRespuesta) {
        obtenerRutinaActivaDelUsuario(id, usuarioRespuesta.id()).eliminar();
    }

    /**
     * Centraliza la autorización: busca simultáneamente ID, propietario y estado no eliminado.
     * Los tres casos no accesibles producen el mismo 404.
     */
    private Rutina obtenerRutinaActivaDelUsuario(int id, Long usuarioId) {
        return rutinaRepository.findByIdAndUsuarioIdAndEliminadaFalse(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "La rutina no existe"));
    }

    /**
     * Construye la respuesta breve de cabecera; no incluye entidades JPA ni el contenido de los días.
     */
    private RutinaDTO convertirDTO(Rutina rutina) {
        return new RutinaDTO(rutina.getNombre(), rutina.getId());
    }

    /**
     * Recupera una rutina propia activa y convierte su contenido dentro de la transacción de lectura.
     * Así se accede a las relaciones diferidas antes de devolver el DTO al controlador.
     *
     * @param id               ID de la rutina
     * @param usuarioRespuesta identidad autenticada
     * @return planificación completa con versión, días y ejercicios
     * @throws ResponseStatusException con estado 404 si la rutina no es accesible
     */
    @Transactional(readOnly = true)
    public RutinaDetalleDTO listarRutinaEntera(
            int id,
            AuthDtos.UsuarioRespuesta usuarioRespuesta
    ) {
        Rutina rutina = obtenerRutinaActivaDelUsuario(id, usuarioRespuesta.id());

        return convertirDetalleDTO(rutina);
    }

    /**
     * Construye el árbol de DTO: rutina, días y entradas de ejercicios.
     * Filtra las alternativas que ya no sean accesibles y no modifica la planificación.
     */
    private RutinaDetalleDTO convertirDetalleDTO(Rutina rutina) {
        List<DiaRutinaDTO> dias = new ArrayList<>();

        for (DiaRutina dia : rutina.getDias()) {
            List<EjercicioRutinaDTO> ejercicios = new ArrayList<>();

            for (EjercicioRutina entrada : dia.getEjercicios()) {
                ejercicios.add(new EjercicioRutinaDTO(
                        entrada.getId(),
                        entrada.getEjercicio().getId(),
                        entrada.getEjercicio().getNombre(),
                        entrada.getSeriesObjetivo(),
                        entrada.getRepeticionesMin(),
                        entrada.getRepeticionesMax(),
                        entrada.getDescansoSeg(),
                        EntrenamientoDtos.EsfuerzoDTO.de(entrada.getEsfuerzoObjetivo()),
                        entrada.getEjercicio().getDescripcionTecnica(),
                        entrada.getTecnicaPersonalizada(),
                        entrada.getAproximaciones().stream().map(EntrenamientoDtos.AproximacionDTO::de).toList(),
                        entrada.getAlternativas().stream()
                                .filter(e -> accesoEjercicio.esAccesible(e, rutina.getUsuario().getId()))
                                .map(EntrenamientoDtos.AlternativaDTO::de).toList()
                ));
            }

            dias.add(new DiaRutinaDTO(
                    dia.getId(),
                    dia.getNombre(),
                    dia.getOrden(),
                    ejercicios
            ));
        }

        return new RutinaDetalleDTO(rutina.getId(), rutina.getVersion(), rutina.getNombre(), dias);
    }

    /**
     * Reemplaza el contenido de una rutina propia activa con la edición completa recibida.
     * Los IDs nulos crean elementos y los existentes deben pertenecer a su rutina y día;
     * los omitidos se eliminan. Las posiciones se asignan según las listas recibidas.
     * Comprueba ejercicios principales y alternativas, aplica las reglas de objetivos y guarda
     * todo en una transacción: un error revierte también los cambios ya enviados mediante flush.
     * No cambia las copias de indicaciones de sesiones iniciadas anteriormente.
     *
     * @param id        ID de la rutina
     * @param datos     nombre, días y entradas completos con la última versión consultada
     * @param usuarioId ID del usuario autenticado
     * @return detalle persistido con IDs definitivos, orden y versión actualizados
     * @throws ResponseStatusException  con estado 404 si un recurso no es accesible, o 409 por versión antigua
     * @throws IllegalArgumentException si los IDs de hijos, objetivos o propuestas no son válidos
     */
    @Transactional
    public RutinaDetalleDTO guardarCompleta(int id, EntrenamientoDtos.GuardarRutinaDTO datos, Long usuarioId) {
        Rutina rutina = obtenerRutinaActivaDelUsuario(id, usuarioId);
        if (rutina.getVersion() != datos.version())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La rutina ha cambiado; vuelve a cargarla");
        java.util.Map<Integer, DiaRutina> originales = new java.util.HashMap<>();
        rutina.getDias().forEach(dia -> originales.put(dia.getId(), dia));
        java.util.Set<Integer> usados = new java.util.HashSet<>();
        for (var dia : datos.dias()) {
            if (dia.id() != null && (!originales.containsKey(dia.id()) || !usados.add(dia.id()))) {
                throw new IllegalArgumentException("Día repetido o ajeno a la rutina");
            }
        }
        rutina.editar(datos.nombre());
        rutina.conservarDias(usados);
        rutina.getDias().forEach(DiaRutina::reservarOrden);
        entityManager.flush();
        int ordenDia = 0;
        for (var entradaDia : datos.dias()) {
            DiaRutina dia = entradaDia.id() == null ? rutina.anadirDia(entradaDia.nombre()) : originales.get(entradaDia.id());
            dia.editar(entradaDia.nombre(), ++ordenDia);
            java.util.Map<Integer, EjercicioRutina> anteriores = new java.util.HashMap<>();
            dia.getEjercicios().forEach(e -> anteriores.put(e.getId(), e));
            java.util.Set<Integer> entradasUsadas = new java.util.HashSet<>();
            for (var entrada : entradaDia.ejercicios()) {
                if (entrada.id() != null && (!anteriores.containsKey(entrada.id()) || !entradasUsadas.add(entrada.id()))) {
                    throw new IllegalArgumentException("Entrada repetida o ajena al día");
                }
            }
            dia.conservarEjercicios(entradasUsadas);
            int ordenEjercicio = 0;
            for (var entrada : entradaDia.ejercicios()) {
                var d = entrada.datos();
                var ejercicio = accesoEjercicio.obtener(d.ejercicioId(), usuarioId);
                EjercicioRutina e = entrada.id() == null
                        ? dia.anadirEjercicio(ejercicio, d.seriesObjetivo(), d.repeticionesMin(), d.repeticionesMax())
                        : anteriores.get(entrada.id());
                e.editar(ejercicio, d.seriesObjetivo(), d.repeticionesMin(), d.repeticionesMax());
                e.configurar(d.descansoSeg(), d.esfuerzoObjetivo() == null ? null : d.esfuerzoObjetivo().dominio(),
                        d.tecnicaPersonalizada(), d.aproximaciones().stream().map(EntrenamientoDtos.AproximacionDTO::dominio).toList());
                e.configurarAlternativas(d.alternativasIds().stream()
                        .map(alternativaId -> accesoEjercicio.obtener(alternativaId, usuarioId)).toList());
                e.ordenar(++ordenEjercicio);
            }
        }
        entityManager.flush();
        entityManager.refresh(rutina);
        return convertirDetalleDTO(rutina);
    }
}
