package dam.com.tfg.mygymbro.mygymbro_api.autenticacion;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.assertThat;

// Comprueba el SQL y la conservación de datos en H2; no sustituye PostgreSQL real.
class PostgresqlMigrationTests {
    @Test void migraElEsquemaPostgresqlConservandoUsuariosYRevocandoCaducidad() throws Exception {
        String url = "jdbc:h2:mem:postgresMigration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/postgresql")
                .target("1").load().migrate();
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var sql = connection.createStatement()) {
            sql.executeUpdate("INSERT INTO usuarios (nombre,email,contrasena_hash) VALUES ('Ana','ana@example.com','hash')");
            sql.executeUpdate("INSERT INTO sesiones (token_hash,usuario_id,expira_en) SELECT 'sesion',id,CURRENT_TIMESTAMP FROM usuarios");
            Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/postgresql")
                    .load().migrate();
            try (var rows = sql.executeQuery("SELECT id,email_verificado,contrasena_hash FROM usuarios")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getLong(1)).isPositive();
                assertThat(rows.getBoolean(2)).isFalse();
                assertThat(rows.getString(3)).isEqualTo("hash");
            }
            try (var rows = sql.executeQuery("SELECT expira_en FROM sesiones")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getTimestamp(1)).isNull();
            }
            sql.executeUpdate("INSERT INTO usuarios (nombre,email) VALUES ('Google','google@example.com')");
            sql.executeUpdate("INSERT INTO verificaciones_correo (token_hash,usuario_id,creado_en,expira_en) SELECT 'token',id,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP FROM usuarios WHERE email='ana@example.com'");
        }
    }
}
