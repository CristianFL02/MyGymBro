package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "dias_rutina")
public class DiaRutina {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rutina_id")
    private Rutina rutina;
    @Column(nullable = false, length = 15)
    private String nombre;
    @Column(nullable = false)
    private int orden;
    @OneToMany(mappedBy = "diaRutina", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<EjercicioRutina> ejercicios = new ArrayList<>();

    protected DiaRutina() {}

    DiaRutina(Rutina rutina, String nombre, int orden) {
        this.rutina = rutina;
        this.nombre = DatosEntrenamiento.texto(nombre, 15);
        this.orden = orden;
    }

    public EjercicioRutina anadirEjercicio(Ejercicio ejercicio, int series, int repeticiones) {
        rutina.comprobarActiva();
        EjercicioRutina entrada = new EjercicioRutina(this, ejercicio, series, repeticiones);
        ejercicios.add(entrada);
        return entrada;
    }

    public void quitarEjercicio(EjercicioRutina ejercicioRutina) {
        rutina.comprobarActiva();
        if (!ejercicios.remove(ejercicioRutina)) {
            throw new IllegalArgumentException("El ejercicio no pertenece a este día");
        }
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public int getOrden() { return orden; }
    public List<EjercicioRutina> getEjercicios() { return List.copyOf(ejercicios); }
}
