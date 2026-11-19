package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento;

import jakarta.persistence.*;

/** Copia de las indicaciones al iniciar la sesión; no depende de futuras ediciones de la rutina. */
@Embeddable
public class PlanRegistro {
    private int seriesObjetivo;
    private int repeticionesMin;
    private int repeticionesMax;
    private Integer descansoSeg;
    @Embedded
    private Esfuerzo esfuerzoObjetivo;
    @Column(length = 2000)
    private String tecnicaOriginal;
    @Column(length = 2000)
    private String tecnicaPersonalizada;

    protected PlanRegistro() {}

    public PlanRegistro(int series, int minimo, int maximo, Integer descanso, Esfuerzo esfuerzo,
                        String original, String personalizada) {
        if (series <= 0 || minimo < 0 || maximo < minimo || (descanso != null && descanso < 0)
                || (original != null && original.length() > 2000)
                || (personalizada != null && personalizada.length() > 2000)) {
            throw new IllegalArgumentException("Los objetivos o la técnica del ejercicio no son válidos");
        }
        seriesObjetivo = series;
        repeticionesMin = minimo;
        repeticionesMax = maximo;
        descansoSeg = descanso;
        esfuerzoObjetivo = esfuerzo;
        tecnicaOriginal = original;
        tecnicaPersonalizada = personalizada;
    }

    public static PlanRegistro de(EjercicioRutina entrada) {
        return new PlanRegistro(entrada.getSeriesObjetivo(), entrada.getRepeticionesMin(),
                entrada.getRepeticionesMax(), entrada.getDescansoSeg(), entrada.getEsfuerzoObjetivo(),
                entrada.getEjercicio().getDescripcionTecnica(), entrada.getTecnicaPersonalizada());
    }

    public int getSeriesObjetivo() { return seriesObjetivo; }
    public int getRepeticionesMin() { return repeticionesMin; }
    public int getRepeticionesMax() { return repeticionesMax; }
    public Integer getDescansoSeg() { return descansoSeg; }
    public Esfuerzo getEsfuerzoObjetivo() { return esfuerzoObjetivo; }
    public String getTecnicaOriginal() { return tecnicaOriginal; }
    public String getTecnicaPersonalizada() { return tecnicaPersonalizada; }
}
