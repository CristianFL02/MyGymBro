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
