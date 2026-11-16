package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento;

import jakarta.persistence.*;
import dam.com.tfg.mygymbro.mygymbro_api.usuario.Usuario;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "sesiones_entrenamiento")
public class SesionEntrenamiento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(nullable = false, columnDefinition = "timestamp(6)")
    private Instant fecha;
    private int duracionMin;
    private int sensacion;
    private boolean finalizada;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rutina_id")
    private Rutina rutina;
    @OneToMany(mappedBy = "sesion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<RegistroEjercicio> registros = new ArrayList<>();

    protected SesionEntrenamiento() {}

    public SesionEntrenamiento(Usuario usuario, Rutina rutina, Instant fecha, int duracionMin, int sensacion) {
        if (duracionMin < 0) throw new IllegalArgumentException("Duración negativa");
        this.usuario = Objects.requireNonNull(usuario);
        this.rutina = rutina;
        this.fecha = Objects.requireNonNull(fecha);
        this.duracionMin = duracionMin;
        this.sensacion = sensacion;
    }

    public RegistroEjercicio anadirRegistro(Ejercicio ejercicio) {
        RegistroEjercicio registro = new RegistroEjercicio(this, ejercicio);
        registros.add(registro);
        return registro;
    }

    public void modificarRegistro(RegistroEjercicio registro) {
        for (RegistroEjercicio actual : registros) {
            if (actual == registro) return;
            if (actual.getId() != 0 && actual.getId() == registro.getId()) {
                actual.copiarSeriesDe(registro);
                return;
            }
        }
        throw new IllegalArgumentException("El registro no pertenece a esta sesión");
    }

    public void finalizar() {
        if (registros.stream().anyMatch(registro -> registro.getSeries().isEmpty())) {
            throw new IllegalStateException("Cada ejercicio registrado debe tener al menos una serie");
        }
        finalizada = true;
    }

    public int getId() { return id; }
    public Instant getFecha() { return fecha; }
    public int getDuracionMin() { return duracionMin; }
    public int getSensacion() { return sensacion; }
    public boolean isFinalizada() { return finalizada; }
    public Usuario getUsuario() { return usuario; }
    public Rutina getRutina() { return rutina; }
    public List<RegistroEjercicio> getRegistros() { return List.copyOf(registros); }
}
