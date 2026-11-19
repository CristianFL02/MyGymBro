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
    private Integer diaRutinaOrigenId;
    private String nombreDiaOrigen;
    @Version
    private long version;
    private Instant ultimaModificacion;
    @OneToMany(mappedBy = "sesion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC, id ASC")
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
        if (registros.isEmpty()) throw new IllegalStateException("La sesión necesita al menos un ejercicio registrado");
        if (registros.stream().anyMatch(registro -> registro.getSeries().isEmpty())) {
            throw new IllegalStateException("Cada ejercicio registrado debe tener al menos una serie");
        }
        finalizada = true;
    }

    public void copiarDia(DiaRutina dia) {
        if (!registros.isEmpty() || diaRutinaOrigenId != null) throw new IllegalStateException("La sesión ya está iniciada");
        diaRutinaOrigenId = dia.getId();
        nombreDiaOrigen = dia.getNombre();
        for (EjercicioRutina entrada : dia.getEjercicios()) {
            RegistroEjercicio registro = anadirRegistro(entrada.getEjercicio());
            registro.configurar(PlanRegistro.de(entrada), entrada.getAproximaciones(), null);
            registro.copiarPropuesta(entrada.getEjercicio(), entrada.getAlternativas());
        }
    }

    public void guardarBorrador(int duracionMin) {
        if (finalizada) throw new IllegalStateException("No se puede editar una sesión finalizada");
        if (duracionMin < 0) throw new IllegalArgumentException("Duración negativa");
        this.duracionMin = duracionMin;
        // Marca también las modificaciones de hijos para el control de concurrencia.
        ultimaModificacion = Instant.now();
    }

    public void conservarRegistros(Set<Integer> ids) {
        registros.removeIf(registro -> !ids.contains(registro.getId()));
    }

    public Integer getDiaRutinaOrigenId() { return diaRutinaOrigenId; }
    public String getNombreDiaOrigen() { return nombreDiaOrigen; }
    public long getVersion() { return version; }

    public int getId() { return id; }
    public Instant getFecha() { return fecha; }
    public int getDuracionMin() { return duracionMin; }
    public int getSensacion() { return sensacion; }
    public boolean isFinalizada() { return finalizada; }
    public Usuario getUsuario() { return usuario; }
    public Rutina getRutina() { return rutina; }
    public List<RegistroEjercicio> getRegistros() { return List.copyOf(registros); }
}
