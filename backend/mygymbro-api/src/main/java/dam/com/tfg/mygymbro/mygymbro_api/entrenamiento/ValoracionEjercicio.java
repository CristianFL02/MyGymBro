package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ValoracionEjercicio {
    private Integer comodidad;
    private Integer capacidadFuerza;
    private Integer bombeo;
    private Integer molestias;
    private Integer calidadTecnica;
    @Column(length = 2000)
    private String comentariosUsuario;

    protected ValoracionEjercicio() {
    }

    public ValoracionEjercicio(Integer comodidad, Integer capacidadFuerza, Integer bombeo,
                               Integer molestias, Integer calidadTecnica, String comentariosUsuario) {
        for (Integer valor : new Integer[]{comodidad, capacidadFuerza, bombeo, molestias, calidadTecnica}) {
            if (valor != null && (valor < 1 || valor > 10)) {
                throw new IllegalArgumentException("Las valoraciones deben estar entre 1 y 10 o sin rellenar");
            }
        }
        if (comentariosUsuario != null && comentariosUsuario.length() > 2000) {
            throw new IllegalArgumentException("Los comentarios admiten hasta 2000 caracteres");
        }
        this.comodidad = comodidad;
        this.capacidadFuerza = capacidadFuerza;
        this.bombeo = bombeo;
        this.molestias = molestias;
        this.calidadTecnica = calidadTecnica;
        this.comentariosUsuario = comentariosUsuario;
    }

    public Integer getComodidad() {
        return comodidad;
    }

    public Integer getCapacidadFuerza() {
        return capacidadFuerza;
    }

    public Integer getBombeo() {
        return bombeo;
    }

    public Integer getMolestias() {
        return molestias;
    }

    public Integer getCalidadTecnica() {
        return calidadTecnica;
    }

    public String getComentariosUsuario() {
        return comentariosUsuario;
    }
}
