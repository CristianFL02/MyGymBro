package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento;

import jakarta.persistence.Embeddable;


@Embeddable
public class Aproximacion {
    private int repeticiones;
    private Double carga;
    private Integer descansoSeg;

    protected Aproximacion() {}

    public Aproximacion(int repeticiones, Double carga, Integer descansoSeg) {
        if (repeticiones <= 0 || (carga != null && (!Double.isFinite(carga) || carga < 0))
                || (descansoSeg != null && descansoSeg < 0)) {
            throw new IllegalArgumentException("La aproximación necesita repeticiones positivas y carga y descanso no negativos");
        }
        this.repeticiones = repeticiones;
        this.carga = carga;
        this.descansoSeg = descansoSeg;
    }

    public int getRepeticiones() { return repeticiones; }
    public Double getCarga() { return carga; }
    public Integer getDescansoSeg() { return descansoSeg; }
    public Aproximacion copiar() { return new Aproximacion(repeticiones, carga, descansoSeg); }
}
