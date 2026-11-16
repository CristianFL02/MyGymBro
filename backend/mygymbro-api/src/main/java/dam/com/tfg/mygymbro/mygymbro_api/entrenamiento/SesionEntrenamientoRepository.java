package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface SesionEntrenamientoRepository extends JpaRepository<SesionEntrenamiento, Integer> {
    List<SesionEntrenamiento> findByUsuarioIdAndFinalizadaTrueAndFechaBetweenOrderByFechaDescIdDesc(
            Long usuarioId, Instant desde, Instant hasta);
}
