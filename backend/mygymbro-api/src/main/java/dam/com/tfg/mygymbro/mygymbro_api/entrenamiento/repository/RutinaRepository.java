package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.repository;

import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.Rutina;
import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.dto.RutinaDTO;
import dam.com.tfg.mygymbro.mygymbro_api.usuario.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Consultas de planificación persistida. Los métodos derivados los implementa Spring Data JPA. */
@Repository
public interface RutinaRepository extends JpaRepository<Rutina, Integer> {
    /** Devuelve solo ID y nombre de las rutinas activas del propietario, sin orden explícito de listado. */
    List<RutinaDTO> findAllByUsuarioIdAndEliminadaFalse(Long id);

    /** Busca una entidad usando simultáneamente ID, propietario y estado activo para aplicar el control de acceso. */
    Optional<Rutina> findByIdAndUsuarioIdAndEliminadaFalse(int id, Long usuarioId);

}
