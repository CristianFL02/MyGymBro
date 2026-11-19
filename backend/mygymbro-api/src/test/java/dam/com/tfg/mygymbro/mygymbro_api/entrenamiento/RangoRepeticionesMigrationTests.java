package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.*;

// Verifica ambos scripts en H2; no sustituye una prueba con los motores reales.
class RangoRepeticionesMigrationTests {
    @ParameterizedTest
    @CsvSource({"MySQL,migration", "PostgreSQL,postgresql"})
    void convierteElObjetivoExistenteEnUnRangoSinPerderDatos(String modo, String carpeta) throws Exception {
        String url = "jdbc:h2:mem:rango" + modo + ";MODE=" + modo + ";DB_CLOSE_DELAY=-1";
        String ubicacion = "classpath:db/" + carpeta;
        Flyway.configure().dataSource(url, "sa", "").locations(ubicacion).target("4").load().migrate();
        try (var conexion = DriverManager.getConnection(url, "sa", "");
             var sql = conexion.createStatement()) {
            sql.executeUpdate("INSERT INTO usuarios (nombre,email,contrasena_hash) VALUES ('Ana','ana@example.com','hash')");
            sql.executeUpdate("INSERT INTO rutinas (nombre,usuario_id,eliminada) SELECT 'Torso',id,FALSE FROM usuarios");
            sql.executeUpdate("INSERT INTO dias_rutina (rutina_id,nombre,orden) SELECT id,'Dia 1',1 FROM rutinas");
            sql.executeUpdate("INSERT INTO ejercicios (nombre,categoria,equipamiento,tipo_medicion,visibilidad) VALUES ('Press','Fuerza','Barra','REPS_CARGA','PUBLICO')");
            sql.executeUpdate("INSERT INTO ejercicios_rutina (dia_rutina_id,ejercicio_id,series_objetivo,repeticiones_objetivo) SELECT d.id,e.id,3,10 FROM dias_rutina d CROSS JOIN ejercicios e");
            sql.executeUpdate("INSERT INTO sesiones_entrenamiento (fecha,duracion_min,sensacion,finalizada,usuario_id) SELECT CURRENT_TIMESTAMP,30,7,TRUE,id FROM usuarios");
            sql.executeUpdate("INSERT INTO registros_ejercicio (sesion_id,ejercicio_id) SELECT s.id,e.id FROM sesiones_entrenamiento s CROSS JOIN ejercicios e");
            sql.executeUpdate("INSERT INTO series_registradas (registro_id,numero,repeticiones,carga,tiempo_seg,distancia_m,rpe,descanso_seg) SELECT id,1,10,20,0,0,8,90 FROM registros_ejercicio");
            Flyway.configure().dataSource(url, "sa", "").locations(ubicacion).load().migrate();
            try (var filas = sql.executeQuery("SELECT series_objetivo,repeticiones_min,repeticiones_max FROM ejercicios_rutina")) {
                assertThat(filas.next()).isTrue();
                assertThat(filas.getInt(1)).isEqualTo(3);
                assertThat(filas.getInt(2)).isEqualTo(10);
                assertThat(filas.getInt(3)).isEqualTo(10);
                assertThat(filas.next()).isFalse();
            }
            assertThatExceptionOfType(java.sql.SQLException.class).isThrownBy(() ->
                    sql.executeUpdate("UPDATE ejercicios_rutina SET repeticiones_min=12,repeticiones_max=8"));
            assertThatExceptionOfType(java.sql.SQLException.class).isThrownBy(() ->
                    sql.executeUpdate("UPDATE ejercicios_rutina SET repeticiones_min=-1"));
            try (var filas = sql.executeQuery("SELECT tipo,rpe,rir FROM series_registradas")) {
                assertThat(filas.next()).isTrue();
                assertThat(filas.getString(1)).isEqualTo("EFECTIVA");
                assertThat(filas.getDouble(2)).isEqualTo(8);
                assertThat(filas.getObject(3)).isNull();
            }
            try (var filas = sql.executeQuery("SELECT comodidad,comentarios_usuario FROM registros_ejercicio")) {
                assertThat(filas.next()).isTrue();
                assertThat(filas.getObject(1)).isNull();
                assertThat(filas.getObject(2)).isNull();
            }
            try (var filas = sql.executeQuery("SELECT ejercicio_propuesto_id FROM registros_ejercicio")) {
                assertThat(filas.next()).isTrue();
                assertThat(filas.getObject(1)).isNull();
            }
            for (String tabla : new String[] {"alternativas_rutina", "alternativas_sesion"}) {
                try (var filas = sql.executeQuery("SELECT COUNT(*) FROM " + tabla)) {
                    assertThat(filas.next()).isTrue();
                    assertThat(filas.getInt(1)).isZero();
                }
            }
        }
    }
}
