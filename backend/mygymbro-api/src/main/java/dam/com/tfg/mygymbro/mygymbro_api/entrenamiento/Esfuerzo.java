package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento;

import jakarta.persistence.Embeddable;

/** RIR y RPE son alternativas; null significa que no se ha indicado esfuerzo. */
@Embeddable
public class Esfuerzo {
    private Integer rir;
    private Double rpe;

    protected Esfuerzo() {}

    public Esfuerzo(Integer rir, Double rpe) {
        if ((rir != null && rpe != null) || (rir != null && rir < 0)
                || (rpe != null && (!Double.isFinite(rpe) || rpe < 0 || rpe > 10))) {
            throw new IllegalArgumentException("Indica RIR no negativo o RPE entre 0 y 10, nunca ambos");
        }
        this.rir = rir;
        this.rpe = rpe;
    }

    public Integer getRir() { return rir; }
    public Double getRpe() { return rpe; }
}
