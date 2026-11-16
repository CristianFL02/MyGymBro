package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento;

import jakarta.persistence.*;
import java.util.Objects;
 
@Entity
@Table(name = "ejercicios_rutina")
public class EjercicioRutina {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dia_rutina_id")
    private DiaRutina diaRutina;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ejercicio_id")
    private Ejercicio ejercicio;
    @Column(nullable = false)
    private int seriesObjetivo;
    @Column(nullable = false)
    private int repeticionesObjetivo;

    protected EjercicioRutina() {}

    EjercicioRutina(DiaRutina diaRutina, Ejercicio ejercicio, int series, int repeticiones) {
        this.diaRutina = diaRutina;
        this.ejercicio = Objects.requireNonNull(ejercicio);
        if (series <= 0 || repeticiones < 0
                || (ejercicio.getTipoMedicion() == TipoMedicion.REPS_CARGA && repeticiones == 0)) {
            throw new IllegalArgumentException("Los objetivos de series y repeticiones no son válidos");
        }
        this.seriesObjetivo = series;
        this.repeticionesObjetivo = repeticiones;
    }

    public int getId() { return id; }
    public Ejercicio getEjercicio() { return ejercicio; }
    public int getSeriesObjetivo() { return seriesObjetivo; }
    public int getRepeticionesObjetivo() { return repeticionesObjetivo; }
}
