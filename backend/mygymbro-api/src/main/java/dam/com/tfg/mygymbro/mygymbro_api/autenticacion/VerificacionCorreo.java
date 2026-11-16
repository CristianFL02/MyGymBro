package dam.com.tfg.mygymbro.mygymbro_api.autenticacion;

import dam.com.tfg.mygymbro.mygymbro_api.usuario.Usuario;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "verificaciones_correo")
public class VerificacionCorreo {
    @Id
    @Column(name = "token_hash", length = 64)
    private String tokenHash;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;
    @Column(name = "creado_en", nullable = false, columnDefinition = "timestamp(6)")
    private Instant creadoEn;
    @Column(name = "expira_en", nullable = false, columnDefinition = "timestamp(6)")
    private Instant expiraEn;

    protected VerificacionCorreo() {}

    VerificacionCorreo(String hash, Usuario usuario, Instant creadoEn, Instant expiraEn) {
        this.tokenHash = hash;
        this.usuario = usuario;
        this.creadoEn = creadoEn;
        this.expiraEn = expiraEn;
    }

    Usuario getUsuario() { return usuario; }
    Instant getCreadoEn() { return creadoEn; }
    Instant getExpiraEn() { return expiraEn; }
}
