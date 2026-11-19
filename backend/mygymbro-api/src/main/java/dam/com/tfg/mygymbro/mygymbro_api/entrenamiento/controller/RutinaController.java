package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.controller;

import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.dto.*;
import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.service.RutinaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import dam.com.tfg.mygymbro.mygymbro_api.autenticacion.AuthDtos.UsuarioRespuesta;

import java.util.List;


/**
 * Entrada HTTP para planificar entrenamientos mediante rutinas propias.
 * Delega las reglas y los permisos en {@link RutinaService}.
 * Una rutina es la planificación; los resultados de un entrenamiento se gestionan en {@link SesionController}.
 */
@RestController
@RequestMapping("api/rutina")
public class RutinaController {
    private RutinaService rutinaService;

    public RutinaController(RutinaService rutinaService) {
        this.rutinaService = rutinaService;
    }

    /**
     * {@code POST /api/rutina/create}: crea una rutina propia con nombre, sin días ni ejercicios.
     * Devuelve 201; el contenido se añade después mediante el guardado completo.
     *
     * @param crearRutinaRequest nombre obligatorio de hasta 30 caracteres
     * @param usuarioRespuesta   identidad obtenida de la sesión de autenticación
     * @return ID y nombre de la rutina creada
     */
    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public RutinaDTO crearRutina(@Valid @RequestBody CrearRutinaDTO crearRutinaRequest,
                                 @AuthenticationPrincipal UsuarioRespuesta usuarioRespuesta) {
        return rutinaService.crearRutina(crearRutinaRequest, usuarioRespuesta);
    }

    /**
     * {@code GET /api/rutina}: carga la lista de rutinas propias no eliminadas.
     * Es la consulta para mostrar los nombres en la pantalla inicial de entrenamiento.
     *
     * @param usuarioRespuesta usuario autenticado propietario de las rutinas
     * @return lista de IDs y nombres; vacía si no tiene rutinas activas
     */
    @GetMapping
    public List<RutinaDTO> listar(@AuthenticationPrincipal UsuarioRespuesta usuarioRespuesta) {
        return rutinaService.listarRutinas(usuarioRespuesta);
    }

    /**
     * {@code PATCH /api/rutina/{id}}: cambia únicamente el nombre de una rutina propia activa.
     * No modifica sus días ni sus ejercicios. Una rutina ajena, eliminada o inexistente produce 404.
     *
     * @param id               ID de la rutina que se quiere renombrar
     * @param editarRutinaDTO  nuevo nombre obligatorio de hasta 30 caracteres
     * @param usuarioRespuesta usuario autenticado
     * @return ID y nombre actualizado
     */
    @PatchMapping("/{id}")
    public RutinaDTO editarNombre(
            @PathVariable int id,
            @Valid @RequestBody EditarRutinaDTO editarRutinaDTO,
            @AuthenticationPrincipal UsuarioRespuesta usuarioRespuesta) {
        return rutinaService.editarNombre(id, editarRutinaDTO, usuarioRespuesta);
    }

    /**
     * {@code POST /api/rutina/{id}/duplicar}: crea otra rutina propia con el mismo nombre y contenido.
     * Copia días, entradas, objetivos, técnicas, aproximaciones y alternativas; no copia sesiones realizadas.
     *
     * @param id               ID de la rutina propia activa de origen
     * @param usuarioRespuesta usuario autenticado
     * @return ID y nombre de la nueva rutina; estado HTTP 201
     */
    @PostMapping("/{id}/duplicar")
    @ResponseStatus(HttpStatus.CREATED)
    public RutinaDTO duplicar(
            @PathVariable int id,
            @AuthenticationPrincipal UsuarioRespuesta usuarioRespuesta) {
        return rutinaService.duplicarRutina(id, usuarioRespuesta);
    }

    /**
     * {@code DELETE /api/rutina/{id}}: marca una rutina propia como eliminada, sin borrar físicamente su fila.
     * Deja de estar disponible en las consultas de rutinas activas. Devuelve 204, sin cuerpo.
     *
     * @param id               ID de la rutina propia activa
     * @param usuarioRespuesta usuario autenticado
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(
            @PathVariable int id,
            @AuthenticationPrincipal UsuarioRespuesta usuarioRespuesta) {
        rutinaService.eliminarRutina(id, usuarioRespuesta);
    }

    /**
     * {@code GET /api/rutina/{id}}: abre una rutina propia activa con todos sus días y ejercicios.
     * Incluye versión, objetivos, técnica, aproximaciones y alternativas; no inicia un entrenamiento.
     *
     * @param id               ID de la rutina elegida en el listado
     * @param usuarioRespuesta usuario autenticado
     * @return detalle completo; una rutina ajena, eliminada o inexistente produce 404
     */
    @GetMapping("/{id}")
    public RutinaDetalleDTO mostrarRutinaEntera(
            @PathVariable int id,
            @AuthenticationPrincipal UsuarioRespuesta usuarioRespuesta
    ) {
        return rutinaService.listarRutinaEntera(id, usuarioRespuesta);
    }

    /**
     * {@code PUT /api/rutina/{id}}: guarda conjuntamente el nombre, días y ejercicios de la planificación.
     * Los IDs nulos crean elementos; los existentes actualizan; los elementos omitidos se eliminan.
     * El orden de las listas determina las posiciones. Una versión antigua produce 409.
     *
     * @param id      ID de la rutina propia activa
     * @param datos   contenido completo y última versión consultada; no son solo los campos cambiados
     * @param usuario usuario autenticado
     * @return detalle persistido con IDs definitivos y versión actualizada
     */
    @PutMapping("/{id}")
    public RutinaDetalleDTO guardarCompleta(@PathVariable int id,
                                            @Valid @RequestBody EntrenamientoDtos.GuardarRutinaDTO datos,
                                            @AuthenticationPrincipal UsuarioRespuesta usuario) {
        return rutinaService.guardarCompleta(id, datos, usuario.id());
    }


}
