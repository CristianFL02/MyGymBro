package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

class EjercicioRutinaTests {
    private Ejercicio ejercicio(TipoMedicion tipo) {
        return new Ejercicio("Ejercicio", "Prueba", new String[0], new String[0],
                "Material", tipo, Visibilidad.PUBLICO, null);
    }

    @Test
    void admiteRangosYObjetivosFijos() {
        var rango = new EjercicioRutina(null, ejercicio(TipoMedicion.REPS_CARGA), 3, 8, 12);
        assertThat(rango.getRepeticionesMin()).isEqualTo(8);
        assertThat(rango.getRepeticionesMax()).isEqualTo(12);
        assertThatCode(() -> new EjercicioRutina(null, ejercicio(TipoMedicion.REPS_CARGA), 3, 10, 10))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({"0,8,12", "-1,8,12", "3,-1,12", "3,12,8", "3,0,12", "3,0,0"})
    void rechazaSeriesORangosInvalidos(int series, int minimo, int maximo) {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new EjercicioRutina(null, ejercicio(TipoMedicion.REPS_CARGA), series, minimo, maximo));
    }

    @Test
    void cambiarUnLimiteNoPermiteInvalidarElRango() {
        var rango = new EjercicioRutina(null, ejercicio(TipoMedicion.REPS_CARGA), 3, 8, 12);
        assertThatIllegalArgumentException().isThrownBy(() -> rango.setRepeticionesMin(13));
        assertThatIllegalArgumentException().isThrownBy(() -> rango.setRepeticionesMin(0));
        assertThatIllegalArgumentException().isThrownBy(() -> rango.setRepeticionesMax(7));
        assertThat(rango.getRepeticionesMin()).isEqualTo(8);
        assertThat(rango.getRepeticionesMax()).isEqualTo(12);
        rango.setRepeticionesMax(15);
        rango.setRepeticionesMin(10);
        assertThat(rango.getRepeticionesMin()).isEqualTo(10);
        assertThat(rango.getRepeticionesMax()).isEqualTo(15);
    }

    @Test
    void mantieneCeroParaEjerciciosMedidosPorTiempoODistancia() {
        for (var tipo : new TipoMedicion[] {TipoMedicion.TIEMPO, TipoMedicion.DISTANCIA_TIEMPO}) {
            assertThatCode(() -> new EjercicioRutina(null, ejercicio(tipo), 3, 0, 0))
                    .doesNotThrowAnyException();
        }
    }
}
