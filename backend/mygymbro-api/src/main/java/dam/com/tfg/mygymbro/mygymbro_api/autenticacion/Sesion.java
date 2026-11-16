package dam.com.tfg.mygymbro.mygymbro_api.autenticacion;

import dam.com.tfg.mygymbro.mygymbro_api.usuario.Usuario;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sesiones")
public class Sesion {
    @Id
    @Column(name = "token_hash", length = 64)
    private String tokenHash;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @Column(name = "expira_en", columnDefinition = "timestamp(6)")
    private Instant expiraEn;

    protected Sesion() {}

    Sesion(String tokenHash, Usuario usuario, Instant expiraEn) {
        this.tokenHash = tokenHash;
        this.usuario = usuario;
        this.expiraEn = expiraEn;
    }

    Usuario getUsuario() { return usuario; }
    Instant getExpiraEn() { return expiraEn; }
}
