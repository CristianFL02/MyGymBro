package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento;

import jakarta.persistence.*;
import dam.com.tfg.mygymbro.mygymbro_api.usuario.Usuario;

import java.util.*;

@Entity
@Table(name = "rutinas")
public class Rutina {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(nullable = false, length = 30)
    private String nombre;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rutina_origen_id")
    private Rutina rutinaOrigen;
    @Column(nullable = false)
    private boolean eliminada;
    @Version
    private long version;
    private java.time.Instant ultimaModificacion;
    @OneToMany(mappedBy = "rutina", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<DiaRutina> dias = new ArrayList<>();

    protected Rutina() {
    }

    public Rutina(String nombre, Usuario usuario) {
        this.usuario = Objects.requireNonNull(usuario);
        editar(nombre);
    }

    public DiaRutina anadirDia(String nombre) {
        comprobarActiva();
        DiaRutina dia = new DiaRutina(this, nombre, dias.size() + 1);
        dias.add(dia);
        return dia;
    }

    public void editar(String nombre) {
        comprobarActiva();
        this.nombre = DatosEntrenamiento.texto(nombre, 30);
        ultimaModificacion = java.time.Instant.now();
    }

    public void eliminar() {
        eliminada = true;
    }

    public Rutina duplicar() {
        comprobarActiva();
        Rutina copia = new Rutina(nombre, usuario);
        copia.rutinaOrigen = this;
        for (DiaRutina dia : dias) {
            DiaRutina diaCopia = copia.anadirDia(dia.getNombre());
            for (EjercicioRutina ejercicio : dia.getEjercicios()) {
                EjercicioRutina entrada = diaCopia.anadirEjercicio(ejercicio.getEjercicio(),
                        ejercicio.getSeriesObjetivo(), ejercicio.getRepeticionesMin(), ejercicio.getRepeticionesMax());
                entrada.configurar(ejercicio.getDescansoSeg(), ejercicio.getEsfuerzoObjetivo(),
                        ejercicio.getTecnicaPersonalizada(), ejercicio.getAproximaciones());
                if (ejercicio.getOrden() > 0) entrada.ordenar(ejercicio.getOrden());
                entrada.configurarAlternativas(ejercicio.getAlternativas());
            }
        }
        return copia;
    }

    void comprobarActiva() {
        if (eliminada) throw new IllegalStateException("La rutina está eliminada");
    }

    public int getId() {
        return id;
    }

    public long getVersion() { return version; }
    public void conservarDias(Set<Integer> ids) {
        comprobarActiva();
        dias.removeIf(dia -> !ids.contains(dia.getId()));
    }

    public String getNombre() {
        return nombre;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Rutina getRutinaOrigen() {
        return rutinaOrigen;
    }

    public boolean isEliminada() {
        return eliminada;
    }

    public List<DiaRutina> getDias() {
        return List.copyOf(dias);
    }
}
