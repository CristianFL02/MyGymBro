package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.service;

import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.*;
import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.dto.EntrenamientoDtos.EjercicioCatalogoDTO;
import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.repository.EjercicioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

/** Aplica la visibilidad del catálogo tanto al consultarlo como al seleccionar ejercicios para guardar datos. */
@Service
public class AccesoEjercicio {
    private final EjercicioRepository ejercicios;
    public AccesoEjercicio(EjercicioRepository ejercicios) { this.ejercicios = ejercicios; }

    /**
     * Resuelve un ejercicio público o privado del usuario indicado.
     * Se utiliza al guardar principales, alternativas y sustituciones para evitar referencias no autorizadas.
     *
     * @param id ID del ejercicio del catálogo
     * @param usuarioId ID del usuario autenticado
     * @return entidad accesible para utilizarla dentro de la transacción del servicio llamante
     * @throws ResponseStatusException con estado 404 si no existe o es privado ajeno
     */
    public Ejercicio obtener(int id, Long usuarioId) {
        return ejercicios.findById(id).filter(e -> esAccesible(e, usuarioId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El ejercicio no existe"));
    }

    /**
     * Evalúa la visibilidad de una entidad ya cargada, sin buscarla de nuevo por ID.
     *
     * @param ejercicio definición del catálogo cuya visibilidad se comprueba
     * @param usuarioId ID del usuario autenticado
     * @return true si es público o el usuario es su creador
     */
    public boolean esAccesible(Ejercicio ejercicio, Long usuarioId) {
        return ejercicio.getVisibilidad() == Visibilidad.PUBLICO
                || (ejercicio.getCreador() != null && usuarioId.equals(ejercicio.getCreador().getId()));
    }

    /**
     * Construye el catálogo disponible para seleccionar ejercicios y alternativas.
     *
     * @param usuarioId ID del usuario autenticado
     * @return ejercicios públicos o creados por el usuario, ordenados por nombre e ID
     */
    @Transactional(readOnly = true)
    public List<EjercicioCatalogoDTO> listar(Long usuarioId) {
        return ejercicios.findByVisibilidadOrCreadorIdOrderByNombreAscIdAsc(Visibilidad.PUBLICO, usuarioId)
                .stream().map(e -> new EjercicioCatalogoDTO(e.getId(), e.getNombre(), e.getDescripcionTecnica(), e.getTipoMedicion())).toList();
    }
}
