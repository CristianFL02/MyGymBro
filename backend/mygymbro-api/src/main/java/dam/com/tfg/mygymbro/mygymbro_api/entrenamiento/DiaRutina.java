package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento;

import jakarta.persistence.*;

import java.util.*;

@Entity
@Table(name = "dias_rutina")
public class DiaRutina {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rutina_id")
    private Rutina rutina;
    @Column(nullable = false, length = 15)
    private String nombre;
    @Column(nullable = false)
    private int orden;
    @OneToMany(mappedBy = "diaRutina", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC, id ASC")
    private List<EjercicioRutina> ejercicios = new ArrayList<>();

    protected DiaRutina() {
    }

    DiaRutina(Rutina rutina, String nombre, int orden) {
        this.rutina = rutina;
        this.nombre = DatosEntrenamiento.texto(nombre, 15);
        this.orden = orden;
    }

    public EjercicioRutina anadirEjercicio(Ejercicio ejercicio, int series, int repeticionesMin, int repeticionesMax) {
        rutina.comprobarActiva();
        EjercicioRutina entrada = new EjercicioRutina(this, ejercicio, series, repeticionesMin, repeticionesMax);
        ejercicios.add(entrada);
        return entrada;
    }

    public void quitarEjercicio(EjercicioRutina ejercicioRutina) {
        rutina.comprobarActiva();
        if (!ejercicios.remove(ejercicioRutina)) {
            throw new IllegalArgumentException("El ejercicio no pertenece a este día");
        }
    }

    public int getId() {
        return id;
    }

    public void reservarOrden() {
        orden = -id;
    }

    public void editar(String nombre, int orden) {
        rutina.comprobarActiva();
        if (orden <= 0) throw new IllegalArgumentException("Orden inválido");
        this.nombre = DatosEntrenamiento.texto(nombre, 15);
        this.orden = orden;
    }

    public void conservarEjercicios(Set<Integer> ids) {
        rutina.comprobarActiva();
        ejercicios.removeIf(entrada -> !ids.contains(entrada.getId()));
    }

    public String getNombre() {
        return nombre;
    }

    public int getOrden() {
        return orden;
    }

    public List<EjercicioRutina> getEjercicios() {
        return List.copyOf(ejercicios);
    }
}
