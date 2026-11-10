package dam.com.tfg.mygymbro.mygymbro_api.autenticacion;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface VerificacionCorreoRepository extends JpaRepository<VerificacionCorreo, String> {
    Optional<VerificacionCorreo> findByUsuarioId(Long usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from VerificacionCorreo v where v.tokenHash = :hash")
    Optional<VerificacionCorreo> findLockedByHash(@Param("hash") String hash);
}
