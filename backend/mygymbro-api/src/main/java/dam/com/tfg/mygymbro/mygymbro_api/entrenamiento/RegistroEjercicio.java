package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "registros_ejercicio")
public class RegistroEjercicio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sesion_id")
    private SesionEntrenamiento sesion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ejercicio_id")
    private Ejercicio ejercicio;
    private int orden;
    @Embedded
    private ValoracionEjercicio valoracion;
    @Embedded
    private PlanRegistro plan;
    @ElementCollection
    @CollectionTable(name = "aproximaciones_sesion", joinColumns = @JoinColumn(name = "registro_id"))
    @OrderColumn(name = "posicion")
    private List<Aproximacion> aproximaciones = new ArrayList<>();
    @ManyToMany
    @JoinTable(name = "alternativas_sesion", joinColumns = @JoinColumn(name = "registro_id"),
            inverseJoinColumns = @JoinColumn(name = "ejercicio_id"))
    @OrderColumn(name = "posicion")
    private List<Ejercicio> alternativas = new ArrayList<>();
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ejercicio_propuesto_id")
    private Ejercicio ejercicioPropuesto;
    @OneToMany(mappedBy = "registro", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numero ASC")
    private List<SerieRegistrada> series = new ArrayList<>();

    protected RegistroEjercicio() {}

    RegistroEjercicio(SesionEntrenamiento sesion, Ejercicio ejercicio) {
        this.sesion = sesion;
        this.ejercicio = Objects.requireNonNull(ejercicio);
    }

    public SerieRegistrada anadirSerie(SerieRegistrada datos) {
        datos.validarMedicion(ejercicio.getTipoMedicion());
        SerieRegistrada serie = datos.copiarPara(this, series.size() + 1);
        series.add(serie);
        return serie;
    }

    public void configurar(PlanRegistro plan, List<Aproximacion> aproximaciones, ValoracionEjercicio valoracion) {
        if (plan != null && ejercicio.getTipoMedicion() == TipoMedicion.REPS_CARGA && plan.getRepeticionesMin() == 0) {
            throw new IllegalArgumentException("El objetivo mínimo debe ser positivo para repeticiones con carga");
        }
        List<Aproximacion> copia = Objects.requireNonNull(aproximaciones).stream().map(Aproximacion::copiar).toList();
        this.plan = plan;
        this.valoracion = valoracion;
        this.aproximaciones.clear();
        this.aproximaciones.addAll(copia);
    }

    public void sustituirSeries(List<SerieRegistrada> nuevas) {
        nuevas.forEach(serie -> serie.validarMedicion(ejercicio.getTipoMedicion()));
        if (sesion.isFinalizada() && nuevas.isEmpty()) throw new IllegalArgumentException("Un registro finalizado necesita series");
        int comunes = Math.min(series.size(), nuevas.size());
        for (int i = 0; i < comunes; i++) series.get(i).actualizar(nuevas.get(i));
        if (series.size() > nuevas.size()) series.subList(nuevas.size(), series.size()).clear();
        for (int i = comunes; i < nuevas.size(); i++) anadirSerie(nuevas.get(i));
    }

    public void sustituirEjercicio(Ejercicio ejercicio) {
        this.ejercicio = Objects.requireNonNull(ejercicio);
    }

    public PlanRegistro getPlan() { return plan; }
    public void copiarPropuesta(Ejercicio principal, List<Ejercicio> alternativas) {
        List<Ejercicio> copia = List.copyOf(alternativas);
        this.ejercicioPropuesto = Objects.requireNonNull(principal);
        this.alternativas.clear();
        this.alternativas.addAll(copia);
    }
    public List<Ejercicio> getAlternativas() { return List.copyOf(alternativas); }
    public Ejercicio getEjercicioPropuesto() { return ejercicioPropuesto; }
    public void ordenar(int orden) { this.orden = orden; }
    public ValoracionEjercicio getValoracion() { return valoracion; }
    public List<Aproximacion> getAproximaciones() { return List.copyOf(aproximaciones); }

    public void modificarSerie(SerieRegistrada serie) {
        SerieRegistrada actual = series.stream()
                .filter(item -> item.getNumero() == serie.getNumero())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No existe ese número de serie"));
        serie.validarMedicion(ejercicio.getTipoMedicion());
        actual.actualizar(serie);
    }

    void copiarSeriesDe(RegistroEjercicio registro) {
        if (ejercicio != registro.ejercicio
                && (ejercicio.getId() == 0 || ejercicio.getId() != registro.ejercicio.getId())) {
            throw new IllegalArgumentException("El ejercicio del registro debe coincidir");
        }
        List<SerieRegistrada> nuevas = registro.getSeries();
        nuevas.forEach(serie -> serie.validarMedicion(ejercicio.getTipoMedicion()));
        if (sesion.isFinalizada() && nuevas.isEmpty()) {
            throw new IllegalArgumentException("Un registro finalizado debe conservar alguna serie");
        }
        for (int i = 0; i < nuevas.size(); i++) {
            if (i < series.size()) series.get(i).actualizar(nuevas.get(i));
            else anadirSerie(nuevas.get(i));
        }
        series.subList(nuevas.size(), series.size()).clear();
    }

    public int getId() { return id; }
    public Ejercicio getEjercicio() { return ejercicio; }
    public List<SerieRegistrada> getSeries() { return List.copyOf(series); }
}
