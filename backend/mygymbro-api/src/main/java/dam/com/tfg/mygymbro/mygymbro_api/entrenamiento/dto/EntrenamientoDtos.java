package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.dto;

import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.List;

public final class EntrenamientoDtos {
    private EntrenamientoDtos() {
    }

    public record EsfuerzoDTO(Integer rir, Double rpe) {
        public Esfuerzo dominio() {
            return new Esfuerzo(rir, rpe);
        }

        public static EsfuerzoDTO de(Esfuerzo valor) {
            return valor == null ? null : new EsfuerzoDTO(valor.getRir(), valor.getRpe());
        }
    }

    public record AproximacionDTO(@Positive int repeticiones, Double carga, Integer descansoSeg) {
        public Aproximacion dominio() {
            return new Aproximacion(repeticiones, carga, descansoSeg);
        }

        public static AproximacionDTO de(Aproximacion a) {
            return new AproximacionDTO(a.getRepeticiones(), a.getCarga(), a.getDescansoSeg());
        }
    }

    public record ValoracionDTO(Integer comodidad, Integer capacidadFuerza, Integer bombeo,
                                Integer molestias, Integer calidadTecnica,
                                @Size(max = 2000) String comentariosUsuario) {
        public ValoracionEjercicio dominio() {
            return new ValoracionEjercicio(comodidad, capacidadFuerza, bombeo, molestias, calidadTecnica, comentariosUsuario);
        }

        public static ValoracionDTO de(ValoracionEjercicio v) {
            return v == null ? null : new ValoracionDTO(v.getComodidad(), v.getCapacidadFuerza(), v.getBombeo(),
                    v.getMolestias(), v.getCalidadTecnica(), v.getComentariosUsuario());
        }
    }

    public record PlanDTO(@Positive int seriesObjetivo, @PositiveOrZero int repeticionesMin,
                          @PositiveOrZero int repeticionesMax, @PositiveOrZero Integer descansoSeg,
                          @Valid EsfuerzoDTO esfuerzoObjetivo, @Size(max = 2000) String tecnicaOriginal,
                          @Size(max = 2000) String tecnicaPersonalizada) {
        public PlanRegistro dominio() {
            return new PlanRegistro(seriesObjetivo, repeticionesMin, repeticionesMax, descansoSeg,
                    esfuerzoObjetivo == null ? null : esfuerzoObjetivo.dominio(), tecnicaOriginal, tecnicaPersonalizada);
        }

        public static PlanDTO de(PlanRegistro p) {
            return p == null ? null : new PlanDTO(p.getSeriesObjetivo(), p.getRepeticionesMin(), p.getRepeticionesMax(),
                    p.getDescansoSeg(), EsfuerzoDTO.de(p.getEsfuerzoObjetivo()), p.getTecnicaOriginal(), p.getTecnicaPersonalizada());
        }
    }

    public record SerieDTO(@NotNull TipoSerie tipo, int repeticiones, double carga, int tiempoSeg,
                           double distanciaM, @Valid EsfuerzoDTO esfuerzo, int descansoSeg, String notas) {
        public SerieRegistrada dominio(int numero) {
            return new SerieRegistrada(numero, repeticiones, carga, tiempoSeg, distanciaM,
                    esfuerzo == null ? null : esfuerzo.dominio(), descansoSeg, notas, tipo);
        }

        public static SerieDTO de(SerieRegistrada s) {
            return new SerieDTO(s.getTipo(), s.getRepeticiones(), s.getCarga(), s.getTiempoSeg(), s.getDistanciaM(),
                    s.getRir() == null && s.getRpe() == null ? null : new EsfuerzoDTO(s.getRir(), s.getRpe()),
                    s.getDescansoSeg(), s.getNotas());
        }
    }

    public record IniciarSesionDTO(@Positive int rutinaId, @Positive int diaId) {
    }

    public record RegistroEntradaDTO(@Positive Integer id, @Positive int ejercicioId, @Valid PlanDTO plan,
                                     @NotNull @Size(max = 100) List<@NotNull @Valid AproximacionDTO> aproximaciones,
                                     @Valid ValoracionDTO valoracion,
                                     @NotNull @Size(max = 200) List<@NotNull @Valid SerieDTO> series) {
    }

    public record GuardarSesionDTO(@PositiveOrZero long version, @PositiveOrZero int duracionMin,
                                   boolean finalizar,
                                   @NotNull @Size(max = 100) List<@NotNull @Valid RegistroEntradaDTO> ejercicios) {
    }

    public record RegistroDTO(int id, int ejercicioId, String nombre, PlanDTO plan,
                              List<AproximacionDTO> aproximaciones, ValoracionDTO valoracion, List<SerieDTO> series,
                              List<AlternativaDTO> alternativas, AlternativaDTO ejercicioPropuesto) {
    }

    public record AlternativaDTO(int ejercicioId, String nombre, TipoMedicion tipoMedicion) {
        public static AlternativaDTO de(Ejercicio ejercicio) {
            return new AlternativaDTO(ejercicio.getId(), ejercicio.getNombre(), ejercicio.getTipoMedicion());
        }
    }

    public record SesionDTO(int id, long version, Integer rutinaId, Integer diaRutinaOrigenId,
                            String nombreDiaOrigen, Instant fecha, int duracionMin, boolean finalizada,
                            List<RegistroDTO> ejercicios) {
    }

    public record EjercicioCatalogoDTO(int id, String nombre, String descripcionTecnica, TipoMedicion tipoMedicion) {
    }

    public record ConfigurarEjercicioDTO(@Positive int ejercicioId, @Positive int seriesObjetivo,
                                         @PositiveOrZero int repeticionesMin, @PositiveOrZero int repeticionesMax,
                                         @PositiveOrZero Integer descansoSeg, @Valid EsfuerzoDTO esfuerzoObjetivo,
                                         @Size(max = 2000) String tecnicaPersonalizada,
                                         @NotNull @Size(max = 100) List<@NotNull @Valid AproximacionDTO> aproximaciones,
                                         @Size(max = 20) List<@NotNull @Positive Integer> alternativasIds) {
        public ConfigurarEjercicioDTO {
            alternativasIds = alternativasIds == null ? List.of() : alternativasIds;
        }

        public ConfigurarEjercicioDTO(int ejercicioId, int seriesObjetivo, int repeticionesMin, int repeticionesMax,
                                      Integer descansoSeg, EsfuerzoDTO esfuerzoObjetivo, String tecnicaPersonalizada,
                                      List<AproximacionDTO> aproximaciones) {
            this(ejercicioId, seriesObjetivo, repeticionesMin, repeticionesMax, descansoSeg,
                    esfuerzoObjetivo, tecnicaPersonalizada, aproximaciones, List.of());
        }
    }

    public record EntradaRutinaDTO(@Positive Integer id, @NotNull @Valid ConfigurarEjercicioDTO datos) {
    }

    public record DiaEntradaDTO(@Positive Integer id, @NotBlank @Size(max = 15) String nombre,
                                @NotNull @Size(max = 100) List<@NotNull @Valid EntradaRutinaDTO> ejercicios) {
    }

    public record GuardarRutinaDTO(@PositiveOrZero long version, @NotBlank @Size(max = 30) String nombre,
                                   @NotNull @Size(max = 100) List<@NotNull @Valid DiaEntradaDTO> dias) {
    }
}
