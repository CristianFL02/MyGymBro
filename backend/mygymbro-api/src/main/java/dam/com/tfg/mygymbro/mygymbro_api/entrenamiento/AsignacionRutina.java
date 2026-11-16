package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento;

import jakarta.persistence.*;
import dam.com.tfg.mygymbro.mygymbro_api.usuario.Usuario;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "asignaciones_rutina")
public class AsignacionRutina {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rutina_id")
    private Rutina rutina;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id")
    private Usuario cliente;
    @Column(nullable = false)
    private LocalDate fechaInicio;

    protected AsignacionRutina() {
    }

    public AsignacionRutina(Rutina rutina, Usuario cliente, LocalDate fechaInicio) {
        this.rutina = Objects.requireNonNull(rutina);
        rutina.comprobarActiva();
        if (rutina.getDias().isEmpty()) {
            throw new IllegalArgumentException("La rutina debe tener al menos un día");
        }
        this.cliente = Objects.requireNonNull(cliente);
        this.fechaInicio = Objects.requireNonNull(fechaInicio);
    }

    public int getId() {
        return id;
    }

    public Rutina getRutina() {
        return rutina;
    }

    public Usuario getCliente() {
        return cliente;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }
}
