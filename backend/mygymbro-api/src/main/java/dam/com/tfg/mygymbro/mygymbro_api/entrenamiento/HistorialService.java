package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento;

import dam.com.tfg.mygymbro.mygymbro_api.usuario.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class HistorialService {
    private final SesionEntrenamientoRepository sesiones;
    private final Clock clock;

    public HistorialService(SesionEntrenamientoRepository sesiones, Clock clock) {
        this.sesiones = sesiones;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<SesionEntrenamiento> obtenerPorSemanas(Usuario usuario, int semanas) {
        if (semanas <= 0 || usuario.getId() == null) {
            throw new IllegalArgumentException("Se necesita un usuario guardado y un número positivo de semanas");
        }
        Instant hasta = clock.instant();
        return sesiones.findByUsuarioIdAndFinalizadaTrueAndFechaBetweenOrderByFechaDescIdDesc(
                usuario.getId(), hasta.minus(7L * semanas, ChronoUnit.DAYS), hasta);
    }

    public Comparativa compararEjercicio(SesionEntrenamiento s1, SesionEntrenamiento s2, Ejercicio ejercicio) {
        List<SerieRegistrada> primeras = seriesDelEjercicio(s1, ejercicio);
        List<SerieRegistrada> segundas = seriesDelEjercicio(s2, ejercicio);
        return new Comparativa(ejercicio, cargaMaxima(segundas) - cargaMaxima(primeras),
                repeticiones(segundas) - repeticiones(primeras), volumen(segundas) - volumen(primeras));
    }

    private List<SerieRegistrada> seriesDelEjercicio(SesionEntrenamiento sesion, Ejercicio ejercicio) {
        List<SerieRegistrada> series = sesion.getRegistros().stream()
                .filter(registro -> registro.getEjercicio() == ejercicio
                        || (ejercicio.getId() != 0 && registro.getEjercicio().getId() == ejercicio.getId()))
                .flatMap(registro -> registro.getSeries().stream())
                .toList();
        if (series.isEmpty()) throw new IllegalArgumentException("El ejercicio necesita series en ambas sesiones");
        return series;
    }

    private double cargaMaxima(List<SerieRegistrada> series) {
        return series.stream().mapToDouble(SerieRegistrada::getCarga).max().orElseThrow();
    }

    private int repeticiones(List<SerieRegistrada> series) {
        return series.stream().mapToInt(SerieRegistrada::getRepeticiones).sum();
    }

    private double volumen(List<SerieRegistrada> series) {
        return series.stream().mapToDouble(serie -> serie.getCarga() * serie.getRepeticiones()).sum();
    }
}
