package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.repository;

import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.SesionEntrenamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Persistencia de entrenamientos realizados o en curso, independiente de la planificación de rutinas. */
public interface SesionEntrenamientoRepository extends JpaRepository<SesionEntrenamiento, Integer> {
    /** Busca una sesión del propietario indicado; admite tanto borradores como sesiones finalizadas. */
    Optional<SesionEntrenamiento> findByIdAndUsuarioId(int id, Long usuarioId);
    /** Recupera los borradores del usuario, de más recientes a más antiguos, con ID como desempate. */
    List<SesionEntrenamiento> findByUsuarioIdAndFinalizadaFalseOrderByFechaDescIdDesc(Long usuarioId);
    /** Recupera solo sesiones finalizadas del usuario, incluyendo ambos límites del intervalo temporal. */
    List<SesionEntrenamiento> findByUsuarioIdAndFinalizadaTrueAndFechaBetweenOrderByFechaDescIdDesc(
            Long usuarioId, Instant desde, Instant hasta);
    @Query("""
    SELECT s
    FROM SesionEntrenamiento s
    WHERE s.usuario.id = :usuarioId
      AND s.finalizada = true
      AND s.fecha >= :inicio
      AND s.fecha < :fin
    ORDER BY s.fecha ASC, s.id ASC
    """)
    List<SesionEntrenamiento> buscarTerminadasDelDia(
            @Param("usuarioId") Long usuarioId,
            @Param("inicio") Instant inicio,
            @Param("fin") Instant fin
    );
}

