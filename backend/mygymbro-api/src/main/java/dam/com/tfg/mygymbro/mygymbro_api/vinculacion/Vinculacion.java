package dam.com.tfg.mygymbro.mygymbro_api.vinculacion;

import dam.com.tfg.mygymbro.mygymbro_api.usuario.Usuario;
import jakarta.persistence.*;

import java.util.Date;

@Entity
public class Vinculacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;
    @Enumerated(EnumType.STRING)
    TipoVinculacion tipoVinculacion;
    @Enumerated(EnumType.STRING)
    EstadoVinculacion estadoVinculacion;
    Date fechaSolicitud;

    @ManyToOne(optional = false)
    @JoinColumn(name = "entrenador_id")
    private Usuario entrenador;
    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id")
    private Usuario cliente;

    public Vinculacion() {
    }


    public Vinculacion(TipoVinculacion tipoVinculacion, Usuario entrenador, Usuario cliente) {
        this.tipoVinculacion = tipoVinculacion;
        this.entrenador = entrenador;
        this.cliente = cliente;
        this.estadoVinculacion = EstadoVinculacion.PENDIENTE;
        this.fechaSolicitud = new Date();
    }

    private void comprobarPendiente() {
        if (this.estadoVinculacion != EstadoVinculacion.PENDIENTE)
            throw new IllegalStateException("La solicitud ya se ha resuelto");
    }

    public int getId() {
        return id;
    }

    public TipoVinculacion getTipoVinculacion() {
        return tipoVinculacion;
    }

    public EstadoVinculacion getEstadoVinculacion() {
        return estadoVinculacion;
    }

    public Date getFechaSolicitud() {
        return fechaSolicitud;
    }

    public Usuario getEntrenador() {
        return entrenador;
    }

    public Usuario getCliente() {
        return cliente;
    }

    protected void aceptar() {
        comprobarPendiente();
        this.estadoVinculacion = EstadoVinculacion.ACEPTADA;
    }


    protected void rechazar() {
        comprobarPendiente();
        this.estadoVinculacion = EstadoVinculacion.RECHAZADA;
    }
}
