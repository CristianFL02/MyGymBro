package dam.com.tfg.mygymbro.mygymbro_api.vinculacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface VinculacionRepository extends JpaRepository<Vinculacion,Integer> {
    boolean existsByEntrenadorIdAndClienteIdAndTipoVinculacionAndEstadoVinculacionIn(
            Long entrenadorId, Long clienteId, TipoVinculacion tipo, Collection<EstadoVinculacion> estados);

    List<Vinculacion> findByClienteIdAndEstadoVinculacionOrderByFechaSolicitudDescIdDesc(
            Long clienteId, EstadoVinculacion estado);

    List<Vinculacion> findByEntrenadorIdAndEstadoVinculacionOrderByClienteIdAsc(
            Long entrenadorId, EstadoVinculacion estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vinculacion v where v.id = :id")
    Optional<Vinculacion> findLockedById(@Param("id") int id);
}
