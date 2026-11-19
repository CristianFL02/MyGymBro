package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.service;

import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.*;
import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.dto.SesionResumenDTO;
import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.repository.SesionEntrenamientoRepository;
import dam.com.tfg.mygymbro.mygymbro_api.usuario.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Lógica interna de historial y comparación; todavía no está expuesta por un controlador.
 * Recibe entidades de dominio, no contratos HTTP. El futuro servicio de aplicación deberá
 * comprobar la autorización antes de comparar sesiones y convertir el resultado a DTO.
 */
@Service
public class HistorialService {
    private final SesionEntrenamientoRepository sesionEntrenamientoRepository;
    private final Clock clock;

    public HistorialService(SesionEntrenamientoRepository sesiones, Clock clock) {
        this.sesionEntrenamientoRepository = sesiones;
        this.clock = clock;
    }

    /**
     * Busca sesiones finalizadas del usuario entre el instante actual y las últimas N semanas.
     * Cada semana equivale aquí a siete días; no representa semanas de calendario.
     *
     * @param usuario usuario persistido cuyos datos se consultan; el llamante debe autorizar el acceso
     * @param semanas número positivo de semanas hacia atrás
     * @return entidades de sesión ordenadas por fecha descendente e ID descendente
     * @throws IllegalArgumentException si el usuario no tiene ID o las semanas no son positivas
     */
    @Transactional(readOnly = true)
    public List<SesionEntrenamiento> obtenerPorSemanas(Usuario usuario, int semanas) {
        if (semanas <= 0 || usuario.getId() == null) {
            throw new IllegalArgumentException("Se necesita un usuario guardado y un número positivo de semanas");
        }
        Instant hasta = clock.instant();
        return sesionEntrenamientoRepository.findByUsuarioIdAndFinalizadaTrueAndFechaBetweenOrderByFechaDescIdDesc(
                usuario.getId(), hasta.minus(7L * semanas, ChronoUnit.DAYS), hasta);
    }

    /**
     * Calcula diferencias de carga máxima, repeticiones totales y volumen del ejercicio indicado.
     * La diferencia es segunda sesión menos primera; solo considera series efectivas.
     * El volumen se calcula sumando carga por repeticiones, no con tiempo o distancia.
     * Este método no recupera sesiones, no comprueba propietario ni exige que estén finalizadas.
     *
     * @param s1        primera sesión de referencia, con registros y series accesibles
     * @param s2        segunda sesión, con registros y series accesibles
     * @param ejercicio ejercicio común que se quiere comparar
     * @return diferencias calculadas; no es todavía una respuesta HTTP
     * @throws IllegalArgumentException si alguna sesión no tiene series efectivas de ese ejercicio
     */
    public Comparativa compararEjercicio(SesionEntrenamiento s1, SesionEntrenamiento s2, Ejercicio ejercicio) {
        List<SerieRegistrada> primeras = seriesDelEjercicio(s1, ejercicio);
        List<SerieRegistrada> segundas = seriesDelEjercicio(s2, ejercicio);
        return new Comparativa(ejercicio, cargaMaxima(segundas) - cargaMaxima(primeras),
                repeticiones(segundas) - repeticiones(primeras), volumen(segundas) - volumen(primeras));
    }

    /**
     * Selecciona el ejercicio por referencia o ID persistido y excluye todas las aproximaciones.
     */
    private List<SerieRegistrada> seriesDelEjercicio(SesionEntrenamiento sesion, Ejercicio ejercicio) {
        List<SerieRegistrada> series = sesion.getRegistros().stream()
                .filter(registro -> registro.getEjercicio() == ejercicio
                        || (ejercicio.getId() != 0 && registro.getEjercicio().getId() == ejercicio.getId()))
                .flatMap(registro -> registro.getSeries().stream())
                .filter(serie -> serie.getTipo() == TipoSerie.EFECTIVA)
                .toList();
        if (series.isEmpty()) throw new IllegalArgumentException("El ejercicio necesita series en ambas sesiones");
        return series;
    }

    /**
     * Obtiene la mayor carga de una lista de series efectivas no vacía.
     */
    private double cargaMaxima(List<SerieRegistrada> series) {
        return series.stream().mapToDouble(SerieRegistrada::getCarga).max().orElseThrow();
    }

    /**
     * Suma las repeticiones de todas las series seleccionadas.
     */
    private int repeticiones(List<SerieRegistrada> series) {
        return series.stream().mapToInt(SerieRegistrada::getRepeticiones).sum();
    }

    /**
     * Suma carga multiplicada por repeticiones; no representa una medida para ejercicios de tiempo o distancia.
     */
    private double volumen(List<SerieRegistrada> series) {
        return series.stream().mapToDouble(serie -> serie.getCarga() * serie.getRepeticiones()).sum();
    }


    private SesionResumenDTO convertirResumenDTO(
            SesionEntrenamiento sesion
    ) {
        return new SesionResumenDTO(
                sesion.getId(),
                sesion.getFecha(),
                sesion.getNombreDiaOrigen(),
                sesion.getDuracionMin(),
                sesion.getRutina() == null
                        ? null
                        : sesion.getRutina().getId()
        );
    }

    @Transactional(readOnly = true)
    public List<SesionResumenDTO> listarTerminadasDelDia(
            Long usuarioId,
            LocalDate dia
    ) {
        ZoneId zona = ZoneId.of("Europe/Madrid");

        Instant inicio = dia.atStartOfDay(zona).toInstant();
        Instant fin = dia.plusDays(1).atStartOfDay(zona).toInstant();

        return sesionEntrenamientoRepository
                .buscarTerminadasDelDia(
                        usuarioId, inicio, fin
                )
                .stream()
                .map(this::convertirResumenDTO)
                .toList();
    }
}
