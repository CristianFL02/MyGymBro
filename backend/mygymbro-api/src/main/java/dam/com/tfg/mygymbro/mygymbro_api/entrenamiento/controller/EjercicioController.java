package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.controller;

import dam.com.tfg.mygymbro.mygymbro_api.autenticacion.AuthDtos.UsuarioRespuesta;
import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.dto.EntrenamientoDtos.EjercicioCatalogoDTO;
import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.service.AccesoEjercicio;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Catálogo consultable para elegir ejercicios principales, alternativas o sustitutos durante una sesión. */
@RestController
@RequestMapping("/api/ejercicios")
public class EjercicioController {
    private final AccesoEjercicio service;

    public EjercicioController(AccesoEjercicio service) {
        this.service = service;
    }

    /**
     * {@code GET /api/ejercicios}: lista ejercicios públicos y privados del usuario autenticado.
     * No crea ejercicios ni concede acceso a los privados de otras personas.
     *
     * @param usuario usuario autenticado
     * @return ID, nombre, técnica original y tipo de medición; lista vacía si no hay ejercicios accesibles
     */
    @GetMapping
    public List<EjercicioCatalogoDTO> listar(@AuthenticationPrincipal UsuarioRespuesta usuario) {
        return service.listar(usuario.id());
    }
}
