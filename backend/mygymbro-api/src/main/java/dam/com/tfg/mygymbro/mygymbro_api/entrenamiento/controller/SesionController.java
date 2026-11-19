package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.controller;

import dam.com.tfg.mygymbro.mygymbro_api.autenticacion.AuthDtos.UsuarioRespuesta;
import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.dto.EntrenamientoDtos.*;
import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.dto.SesionResumenDTO;
import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.service.HistorialService;
import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.service.SesionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Entrada HTTP para realizar un entrenamiento, guardar su borrador y finalizarlo.
 * Sus escrituras afectan a la sesión del usuario, no a la rutina que se utilizó para iniciarla.
 */
@RestController
@RequestMapping("/api/sesiones")
public class SesionController {
    private final SesionService service;
    private final HistorialService historialService;

    public SesionController(SesionService service, HistorialService historialService) {
        this.service = service;
        this.historialService = historialService;
    }

    /**
     * {@code POST /api/sesiones}: inicia un entrenamiento desde un día de una rutina propia activa.
     * Copia indicaciones y propuestas, pero deja vacías las series realizadas. Devuelve 201.
     * El inicio desde rutinas asignadas por entrenadores sigue pendiente.
     *
     * @param datos   IDs de la rutina y del día que se va a entrenar
     * @param usuario usuario autenticado que será propietario de la sesión
     * @return sesión sin finalizar, con ID, versión y ejercicios copiados
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SesionDTO iniciar(@Valid @RequestBody IniciarSesionDTO datos, @AuthenticationPrincipal UsuarioRespuesta usuario) {
        return service.iniciar(datos, usuario.id());
    }

    /**
     * {@code GET /api/sesiones/en-curso}: recupera los entrenamientos propios que se pueden continuar.
     *
     * @param usuario usuario autenticado
     * @return detalles de sesiones sin finalizar, de más recientes a más antiguas
     */
    @GetMapping("/en-curso")
    public List<SesionDTO> enCurso(@AuthenticationPrincipal UsuarioRespuesta usuario) {
        return service.enCurso(usuario.id());
    }

    /**
     * {@code GET /api/sesiones/{id}}: consulta una sesión propia en curso o finalizada.
     *
     * @param id      ID del entrenamiento guardado
     * @param usuario usuario autenticado
     * @return detalle con objetivos copiados, propuestas, valoraciones y series; 404 si no es accesible
     */
    @GetMapping("/{id}")
    public SesionDTO consultar(@PathVariable int id, @AuthenticationPrincipal UsuarioRespuesta usuario) {
        return service.consultar(id, usuario.id());
    }

    /**
     * {@code PUT /api/sesiones/{id}}: guarda el estado completo de una sesión propia en curso.
     * Permite sustituir, añadir, quitar y reordenar ejercicios sin cambiar la rutina original.
     * Con {@code finalizar=false} conserva el borrador; con {@code finalizar=true} intenta terminarlo.
     * No permite editar sesiones ya finalizadas y rechaza versiones antiguas con 409.
     *
     * @param id      ID de la sesión
     * @param datos   versión, duración, ejercicios completos y decisión de finalizar
     * @param usuario usuario autenticado
     * @return detalle guardado con su nueva versión y estado
     */
    @PutMapping("/{id}")
    public SesionDTO guardar(@PathVariable int id, @Valid @RequestBody GuardarSesionDTO datos,
                             @AuthenticationPrincipal UsuarioRespuesta usuario) {
        return service.guardar(id, datos, usuario.id());
    }

    /**
     * @code GET /api/sesiones: consulta una sesión en un día especifico.
     * @param dia   Día que se registró la sesión
     * @param usuario usuario autenticado
     * @return
     */
    @GetMapping
    public List<SesionResumenDTO> listarTerminadasDelDia(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dia,
            @AuthenticationPrincipal UsuarioRespuesta usuario
    ) {
        return historialService.listarTerminadasDelDia(usuario.id(), dia);
    }
}
