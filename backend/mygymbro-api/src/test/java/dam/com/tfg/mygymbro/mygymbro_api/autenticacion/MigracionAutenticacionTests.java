package dam.com.tfg.mygymbro.mygymbro_api.autenticacion;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.assertThat;

class MigracionAutenticacionTests {
    @Test void actualizaLaPrimeraVersionSinBorrarUsuariosNiSesiones() throws Exception {
        String url = "jdbc:h2:mem:migracionAuth;MODE=MySQL;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").target("1").load().migrate();
        try (var connection = DriverManager.getConnection(url, "sa", ""); var sql = connection.createStatement()) {
            sql.executeUpdate("INSERT INTO usuarios (nombre,email,contrasena_hash) VALUES ('Ana','ana@example.com','{pbkdf2}hash-anterior')");
            sql.executeUpdate("INSERT INTO usuario_roles (usuario_id,rol_codigo) SELECT id,'CLIENTE' FROM usuarios");
            sql.executeUpdate("INSERT INTO sesiones (token_hash,usuario_id,expira_en) SELECT 'hash-sesion',id,CURRENT_TIMESTAMP FROM usuarios");
            Flyway.configure().dataSource(url, "sa", "").load().migrate();
            try (var rows = sql.executeQuery("SELECT contrasena_hash,email_verificado FROM usuarios")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("{pbkdf2}hash-anterior");
                assertThat(rows.getBoolean(2)).isFalse();
            }
            try (var rows = sql.executeQuery("SELECT token_hash,expira_en FROM sesiones")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("hash-sesion");
                assertThat(rows.getTimestamp(2)).isNull();
            }
        }
    }
}
