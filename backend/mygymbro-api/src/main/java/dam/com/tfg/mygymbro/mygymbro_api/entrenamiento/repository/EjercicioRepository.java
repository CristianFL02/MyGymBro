package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.repository;

import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.Ejercicio;
import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.Visibilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/** Acceso a definiciones del catálogo; no representa las apariciones de ejercicios dentro de rutinas o sesiones. */
public interface EjercicioRepository extends JpaRepository<Ejercicio, Integer> {
    /** Selecciona ejercicios con la visibilidad indicada o creados por ese usuario, ordenados por nombre e ID. */
    List<Ejercicio> findByVisibilidadOrCreadorIdOrderByNombreAscIdAsc(Visibilidad visibilidad, Long creadorId);
}
