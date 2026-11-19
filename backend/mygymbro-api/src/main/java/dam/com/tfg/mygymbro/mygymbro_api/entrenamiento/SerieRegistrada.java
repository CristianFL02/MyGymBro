package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento;

import jakarta.persistence.*;

@Entity
@Table(name = "series_registradas")
public class SerieRegistrada {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registro_id")
    private RegistroEjercicio registro;
    private int numero;
    private int repeticiones;
    private double carga;
    private int tiempoSeg;
    @Column(name = "distancia_m")
    private double distanciaM;
    @Embedded
    private Esfuerzo esfuerzo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSerie tipo;
    private int descansoSeg;
    private String notas;

    protected SerieRegistrada() {}

    public SerieRegistrada(int numero, int repeticiones, double carga, int tiempoSeg,
                           double distanciaM, int rpe, int descansoSeg, String notas) {
        this(numero, repeticiones, carga, tiempoSeg, distanciaM, new Esfuerzo(null, (double) rpe),
                descansoSeg, notas, TipoSerie.EFECTIVA);
    }

    public SerieRegistrada(int numero, int repeticiones, double carga, int tiempoSeg,
                           double distanciaM, Esfuerzo esfuerzo, int descansoSeg, String notas, TipoSerie tipo) {
        if (numero <= 0 || repeticiones < 0 || tiempoSeg < 0 || descansoSeg < 0
                || !Double.isFinite(carga) || carga < 0 || !Double.isFinite(distanciaM) || distanciaM < 0
                || tipo == null || (notas != null && notas.length() > 255)) {
            throw new IllegalArgumentException("Los datos de la serie no son válidos");
        }
        this.numero = numero;
        this.repeticiones = repeticiones;
        this.carga = carga;
        this.tiempoSeg = tiempoSeg;
        this.distanciaM = distanciaM;
        this.esfuerzo = esfuerzo;
        this.tipo = tipo;
        this.descansoSeg = descansoSeg;
        this.notas = notas;
    }

    void validarMedicion(TipoMedicion tipo) {
        boolean valida = switch (tipo) {
            case REPS_CARGA -> repeticiones > 0;
            case TIEMPO -> tiempoSeg > 0;
            case DISTANCIA_TIEMPO -> distanciaM > 0 && tiempoSeg > 0;
        };
        if (!valida) throw new IllegalArgumentException("Faltan las medidas del ejercicio");
    }

    SerieRegistrada copiarPara(RegistroEjercicio registro, int numero) {
        SerieRegistrada copia = new SerieRegistrada(numero, repeticiones, carga, tiempoSeg,
                distanciaM, esfuerzo, descansoSeg, notas, tipo);
        copia.registro = registro;
        return copia;
    }

    void actualizar(SerieRegistrada datos) {
        repeticiones = datos.repeticiones;
        carga = datos.carga;
        tiempoSeg = datos.tiempoSeg;
        distanciaM = datos.distanciaM;
        esfuerzo = datos.esfuerzo;
        tipo = datos.tipo;
        descansoSeg = datos.descansoSeg;
        notas = datos.notas;
    }

    public int getNumero() { return numero; }
    public int getRepeticiones() { return repeticiones; }
    public double getCarga() { return carga; }
    public int getTiempoSeg() { return tiempoSeg; }
    public double getDistanciaM() { return distanciaM; }
    public Double getRpe() { return esfuerzo == null ? null : esfuerzo.getRpe(); }
    public Integer getRir() { return esfuerzo == null ? null : esfuerzo.getRir(); }
    public TipoSerie getTipo() { return tipo; }
    public int getDescansoSeg() { return descansoSeg; }
    public String getNotas() { return notas; }
}
