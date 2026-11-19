package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento;

import jakarta.persistence.*;

import java.util.Objects;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "ejercicios_rutina")
public class EjercicioRutina {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dia_rutina_id")
    private DiaRutina diaRutina;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ejercicio_id")
    private Ejercicio ejercicio;
    @Column(nullable = false)
    private int seriesObjetivo;
    private int orden;
    @Column(nullable = false)
    private int repeticionesMax;
    @Column(nullable = false)
    private int repeticionesMin;
    private Integer descansoSeg;
    @Embedded
    private Esfuerzo esfuerzoObjetivo;
    @Column(length = 2000)
    private String tecnicaPersonalizada;
    @ElementCollection
    @CollectionTable(name = "aproximaciones_rutina", joinColumns = @JoinColumn(name = "ejercicio_rutina_id"))
    @OrderColumn(name = "posicion")
    private List<Aproximacion> aproximaciones = new ArrayList<>();
    @ManyToMany
    @JoinTable(name = "alternativas_rutina", joinColumns = @JoinColumn(name = "ejercicio_rutina_id"),
            inverseJoinColumns = @JoinColumn(name = "ejercicio_id"))
    @OrderColumn(name = "posicion")
    private List<Ejercicio> alternativas = new ArrayList<>();

    protected EjercicioRutina() {
    }

    EjercicioRutina(DiaRutina diaRutina, Ejercicio ejercicio, int series, int repeticionesMin, int repeticionesMax) {
        this.diaRutina = diaRutina;
        this.ejercicio = Objects.requireNonNull(ejercicio);
        if (series <= 0) {
            throw new IllegalArgumentException("El objetivo de series no es válido");
        }
        validarRango(repeticionesMin, repeticionesMax);
        this.seriesObjetivo = series;
        this.repeticionesMax = repeticionesMax;
        this.repeticionesMin = repeticionesMin;
    }

    public int getId() {
        return id;
    }

    public void configurar(Integer descansoSeg, Esfuerzo esfuerzo, String tecnica,
                           List<Aproximacion> aproximaciones) {
        if (descansoSeg != null && descansoSeg < 0) throw new IllegalArgumentException("Descanso negativo");
        if (tecnica != null && tecnica.length() > 2000)
            throw new IllegalArgumentException("La técnica admite hasta 2000 caracteres");
        List<Aproximacion> copia = Objects.requireNonNull(aproximaciones).stream().map(Aproximacion::copiar).toList();
        this.descansoSeg = descansoSeg;
        this.esfuerzoObjetivo = esfuerzo;
        this.tecnicaPersonalizada = tecnica;
        this.aproximaciones.clear();
        this.aproximaciones.addAll(copia);
    }

    public void editar(Ejercicio ejercicio, int series, int minimo, int maximo) {
        // Validar antes de modificar el estado de esta entrada.
        new EjercicioRutina(diaRutina, ejercicio, series, minimo, maximo);
        this.ejercicio = ejercicio;
        this.seriesObjetivo = series;
        this.repeticionesMin = minimo;
        this.repeticionesMax = maximo;
    }

    public Integer getDescansoSeg() {
        return descansoSeg;
    }

    public void configurarAlternativas(List<Ejercicio> alternativas) {
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        List<Ejercicio> copia = List.copyOf(alternativas);
        for (Ejercicio alternativa : copia) {
            if (alternativa.getId() <= 0 || alternativa.getId() == ejercicio.getId()
                    || !ids.add(alternativa.getId())) {
                throw new IllegalArgumentException("Las alternativas deben ser ejercicios distintos del principal y sin duplicados");
            }
        }
        this.alternativas.clear();
        this.alternativas.addAll(copia);
    }

    public List<Ejercicio> getAlternativas() {
        return List.copyOf(alternativas);
    }

    public void ordenar(int orden) {
        if (orden <= 0) throw new IllegalArgumentException("Orden inválido");
        this.orden = orden;
    }

    public int getOrden() {
        return orden;
    }

    public Esfuerzo getEsfuerzoObjetivo() {
        return esfuerzoObjetivo;
    }

    public String getTecnicaPersonalizada() {
        return tecnicaPersonalizada;
    }

    public String getTecnicaAplicada() {
        return tecnicaPersonalizada != null ? tecnicaPersonalizada : ejercicio.getDescripcionTecnica();
    }

    public List<Aproximacion> getAproximaciones() {
        return List.copyOf(aproximaciones);
    }

    public Ejercicio getEjercicio() {
        return ejercicio;
    }

    public int getSeriesObjetivo() {
        return seriesObjetivo;
    }

    public int getRepeticionesMax() {
        return repeticionesMax;
    }

    public void setRepeticionesMax(int repeticionesMax) {
        validarRango(this.repeticionesMin, repeticionesMax);
        this.repeticionesMax = repeticionesMax;
    }

    public int getRepeticionesMin() {
        return repeticionesMin;
    }

    public void setRepeticionesMin(int repeticionesMin) {
        validarRango(repeticionesMin, this.repeticionesMax);
        this.repeticionesMin = repeticionesMin;
    }

    private void validarRango(int minimo, int maximo) {
        if (minimo < 0 || maximo < minimo
                || (ejercicio.getTipoMedicion() == TipoMedicion.REPS_CARGA && minimo == 0)) {
            throw new IllegalArgumentException("El rango de repeticiones no es válido");
        }
    }
}
