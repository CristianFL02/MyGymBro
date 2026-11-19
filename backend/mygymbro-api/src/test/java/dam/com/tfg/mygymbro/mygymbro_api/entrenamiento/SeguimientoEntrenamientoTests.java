package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento;

import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.repository.SesionEntrenamientoRepository;
import dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.service.HistorialService;
import dam.com.tfg.mygymbro.mygymbro_api.usuario.Usuario;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SeguimientoEntrenamientoTests {
    private Usuario usuario() { return new Usuario("Prueba", "prueba@example.com", "hash", Set.of()); }
    private Ejercicio ejercicio() {
        return new Ejercicio("Press", "Fuerza", new String[0], new String[0], "Barra", TipoMedicion.REPS_CARGA, Visibilidad.PUBLICO, null);
    }

    @Test
    void permiteValoracionesAusentesYExtremosPeroRechazaCeroYOnce() {
        var sinValorar = new ValoracionEjercicio(null, null, null, null, null, null);
        assertThat(sinValorar.getComodidad()).isNull();
        var valorada = new ValoracionEjercicio(1, 10, 1, 10, 1, "Comentario");
        assertThat(valorada.getCapacidadFuerza()).isEqualTo(10);
        assertThatIllegalArgumentException().isThrownBy(() -> new ValoracionEjercicio(null, 0, null, null, null, null));
        assertThatIllegalArgumentException().isThrownBy(() -> new ValoracionEjercicio(null, null, 11, null, null, null));
    }

    @Test
    void aproximacionesNoEntranEnLaComparacionDeSeriesEfectivas() {
        var e = ejercicio();
        var usuario = usuario();
        var primera = new SesionEntrenamiento(usuario, null, Instant.now(), 0, 0);
        var segunda = new SesionEntrenamiento(usuario, null, Instant.now(), 0, 0);
        primera.anadirRegistro(e).anadirSerie(new SerieRegistrada(1, 8, 20, 0, 0, 8, 90, null));
        var registro = segunda.anadirRegistro(e);
        registro.anadirSerie(new SerieRegistrada(1, 2, 100, 0, 0, null, 60, null, TipoSerie.APROXIMACION));
        registro.anadirSerie(new SerieRegistrada(2, 10, 20, 0, 0, new Esfuerzo(2, null), 90, null, TipoSerie.EFECTIVA));
        var servicio = new HistorialService(mock(SesionEntrenamientoRepository.class), Clock.systemUTC());
        var comparacion = servicio.compararEjercicio(primera, segunda, e);
        assertThat(comparacion.diferenciaCarga()).isZero();
        assertThat(comparacion.diferenciaRepeticiones()).isEqualTo(2);
        assertThat(comparacion.diferenciaVolumen()).isEqualTo(40);
    }

    @Test
    void duplicarYEditarIndicacionesNoModificaLaCopiaDeUnaSesionIniciada() {
        var rutina = new Rutina("Base", usuario());
        var dia = rutina.anadirDia("Torso");
        var e = ejercicio();
        e.describirTecnica("Original");
        var entrada = dia.anadirEjercicio(e, 3, 8, 12);
        entrada.configurar(120, new Esfuerzo(2, null), "Personal", List.of(new Aproximacion(10, 10.0, 60)));
        var sesion = new SesionEntrenamiento(rutina.getUsuario(), rutina, Instant.now(), 0, 0);
        sesion.copiarDia(dia);
        var copia = rutina.duplicar().getDias().getFirst().getEjercicios().getFirst();
        entrada.configurar(60, null, "Cambiada", List.of());
        e.describirTecnica("Nueva original");
        var registro = sesion.getRegistros().getFirst();
        assertThat(registro.getPlan().getTecnicaOriginal()).isEqualTo("Original");
        assertThat(registro.getPlan().getTecnicaPersonalizada()).isEqualTo("Personal");
        assertThat(registro.getPlan().getDescansoSeg()).isEqualTo(120);
        assertThat(registro.getAproximaciones()).hasSize(1);
        assertThat(copia.getAproximaciones()).hasSize(1);
        assertThat(copia.getEsfuerzoObjetivo().getRir()).isEqualTo(2);
    }
}
