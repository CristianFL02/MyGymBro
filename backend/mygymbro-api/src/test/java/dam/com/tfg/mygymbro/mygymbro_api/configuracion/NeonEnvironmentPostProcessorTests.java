package dam.com.tfg.mygymbro.mygymbro_api.configuracion;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;

class NeonEnvironmentPostProcessorTests {
    @Test void separaCredencialesYTraduceChannelBindingSinRomperCaracteres() {
        var connection = NeonEnvironmentPostProcessor.parse(
                "postgresql://user:p%40ss+word@db.neon.tech/db?sslmode=require&channel_binding=require");
        assertThat(connection.url()).isEqualTo("jdbc:postgresql://db.neon.tech/db?channelBinding=require&sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory");
        assertThat(connection.password()).isEqualTo("p@ss+word");
        assertThat(connection.username()).isEqualTo("user");
        assertThat(connection.toString()).doesNotContain("p@ss", "user");
    }

    @Test void usaPoolParaJpaYConexionDirectaParaFlyway() {
        var env = new MockEnvironment()
                .withProperty("DATABASE_URL", "postgresql://u:p@db-pooler.neon.tech/db?sslmode=require")
                .withProperty("DATABASE_URL_UNPOOLED", "postgresql://u:p@db.neon.tech/db?sslmode=require");
        env.setActiveProfiles("neon");
        new NeonEnvironmentPostProcessor().postProcessEnvironment(env, new SpringApplication());
        assertThat(env.getProperty("spring.datasource.url")).contains("db-pooler.neon.tech");
        assertThat(env.getProperty("spring.flyway.url")).contains("db.neon.tech").doesNotContain("pooler");
        assertThat(env.getProperty("spring.datasource.username")).isEqualTo("u");
        assertThat(env.getProperty("spring.flyway.user")).isEqualTo("u");
    }

    @Test void noModificaLaConfiguracionMysqlLocal() {
        var env = new MockEnvironment().withProperty("DATABASE_URL", "postgresql://u:p@db.neon.tech/db");
        new NeonEnvironmentPostProcessor().postProcessEnvironment(env, new SpringApplication());
        assertThat(env.getProperty("spring.datasource.url")).isNull();
    }

    @Test void noExponeSecretosEnErroresDeUrl() {
        assertThatThrownBy(() -> NeonEnvironmentPostProcessor.parse("postgresql://u:secret@@bad"))
                .isInstanceOf(IllegalStateException.class).hasMessageNotContaining("secret").hasNoCause();
    }

    @Test void rechazaLasMigracionesSobrePool() {
        var env = new MockEnvironment().withProperty("DATABASE_URL", "postgresql://u:p@db-pooler.neon.tech/db");
        env.setActiveProfiles("render");
        assertThatThrownBy(() -> new NeonEnvironmentPostProcessor().postProcessEnvironment(env, new SpringApplication()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("DATABASE_URL_UNPOOLED");
    }
}
