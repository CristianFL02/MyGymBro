package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento;

import jakarta.persistence.*;
import dam.com.tfg.mygymbro.mygymbro_api.usuario.Usuario;
import java.util.*;

@Entity
@Table(name = "ejercicios")
public class Ejercicio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(nullable = false)
    private String nombre;
    @Column(nullable = false)
    private String categoria;
    @ElementCollection
    @CollectionTable(name = "ejercicio_musculos_primarios", joinColumns = @JoinColumn(name = "ejercicio_id"))
    @OrderColumn(name = "posicion")
    @Column(name = "musculo", nullable = false)
    private List<String> musculosPrimarios = new ArrayList<>();
    @ElementCollection
    @CollectionTable(name = "ejercicio_musculos_secundarios", joinColumns = @JoinColumn(name = "ejercicio_id"))
    @OrderColumn(name = "posicion")
    @Column(name = "musculo", nullable = false)
    private List<String> musculosSecundarios = new ArrayList<>();
    @Column(nullable = false)
    private String equipamiento;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMedicion tipoMedicion;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibilidad visibilidad;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creador_id")
    private Usuario creador;

    protected Ejercicio() {}

    public Ejercicio(String nombre, String categoria, String[] musculosPrimarios,
                     String[] musculosSecundarios, String equipamiento,
                     TipoMedicion tipoMedicion, Visibilidad visibilidad, Usuario creador) {
        this.nombre = DatosEntrenamiento.texto(nombre, 255);
        this.categoria = DatosEntrenamiento.texto(categoria, 255);
        this.musculosPrimarios = copiarMusculos(musculosPrimarios);
        this.musculosSecundarios = copiarMusculos(musculosSecundarios);
        this.equipamiento = DatosEntrenamiento.texto(equipamiento, 255);
        this.tipoMedicion = Objects.requireNonNull(tipoMedicion);
        this.visibilidad = Objects.requireNonNull(visibilidad);
        if (visibilidad == Visibilidad.PRIVADO && creador == null) {
            throw new IllegalArgumentException("Un ejercicio privado necesita un creador");
        }
        this.creador = creador;
    }

    private static List<String> copiarMusculos(String[] musculos) {
        List<String> copia = new ArrayList<>();
        for (String musculo : musculos) {
            copia.add(DatosEntrenamiento.texto(musculo, 255));
        }
        return copia;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCategoria() { return categoria; }
    public String[] getMusculosPrimarios() { return musculosPrimarios.toArray(String[]::new); }
    public String[] getMusculosSecundarios() { return musculosSecundarios.toArray(String[]::new); }
    public String getEquipamiento() { return equipamiento; }
    public TipoMedicion getTipoMedicion() { return tipoMedicion; }
    public Visibilidad getVisibilidad() { return visibilidad; }
    public Usuario getCreador() { return creador; }
}
