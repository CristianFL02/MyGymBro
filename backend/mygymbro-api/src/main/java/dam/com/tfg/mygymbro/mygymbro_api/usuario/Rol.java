package dam.com.tfg.mygymbro.mygymbro_api.usuario;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Entity
@Table(name = "roles")
public class Rol {
    @Id
    @Column(length = 40)
    private String codigo;

    protected Rol() {
    }

    public String getCodigo() {
        return codigo;
    }
}
