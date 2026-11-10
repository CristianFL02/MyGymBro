package dam.com.tfg.mygymbro.mygymbro_api.configuracion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Adapta las URL de Neon a JDBC sin incluir credenciales en la URL registrada. */
public class NeonEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    @Override
    public int getOrder() { return Ordered.HIGHEST_PRECEDENCE + 20; }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.acceptsProfiles(Profiles.of("neon", "render"))) return;
        String direct = first(environment, "DATABASE_URL_UNPOOLED", "DATABASE_URL");
        String pooled = first(environment, "DATABASE_URL_POOLED", "DATABASE_URL");
        if (direct == null && pooled == null) return; // Configuración JDBC DB_* existente.
        Connection directConnection = parse(direct != null ? direct : pooled);
        Connection pooledConnection = parse(pooled != null ? pooled : direct);
        if (directConnection.url().contains("-pooler.")) {
            throw new IllegalStateException("Configura DATABASE_URL_UNPOOLED con la conexión directa para Flyway");
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        put(properties, "spring.datasource", pooledConnection);
        put(properties, "spring.flyway", directConnection);
        environment.getPropertySources().addFirst(new MapPropertySource("neonJdbc", properties));
    }

    private static String first(ConfigurableEnvironment environment, String... keys) {
        for (String key : keys) {
            String value = environment.getProperty(key);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private static void put(Map<String, Object> properties, String prefix, Connection connection) {
        properties.put(prefix + ".url", connection.url());
        properties.put(prefix + (prefix.equals("spring.flyway") ? ".user" : ".username"), connection.username());
        properties.put(prefix + ".password", connection.password());
    }

    static Connection parse(String value) {
        try {
            URI uri = URI.create(value);
            if (!("postgresql".equals(uri.getScheme()) || "postgres".equals(uri.getScheme()))
                    || uri.getHost() == null || uri.getRawUserInfo() == null
                    || uri.getPath() == null || uri.getPath().length() < 2 || uri.getFragment() != null) {
                throw new IllegalArgumentException();
            }
            String[] credentials = uri.getRawUserInfo().split(":", 2);
            if (credentials.length != 2 || credentials[0].isEmpty() || credentials[1].isEmpty()) {
                throw new IllegalArgumentException();
            }
            String query = uri.getRawQuery();
            // libpq usa channel_binding; PostgreSQL JDBC usa channelBinding.
            if (query != null) query = query.replaceAll("(^|&)channel_binding=", "$1channelBinding=");
            // Neon usa certificados públicos: validar cadena y nombre del servidor
            // con el almacén de confianza de Java, sin depender de ~/.postgresql.
            query = query == null ? "" : query.replaceAll("(^|&)sslmode=[^&]*", "$1")
                    .replaceAll("(^|&)sslfactory=[^&]*", "$1").replaceAll("^&+|&+$", "");
            query = (query.isEmpty() ? "" : query + "&")
                    + "sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory";
            String host = uri.getHost();
            if (host.contains(":") && !host.startsWith("[")) host = "[" + host + "]";
            String url = "jdbc:postgresql://" + host + (uri.getPort() == -1 ? "" : ":" + uri.getPort())
                    + uri.getRawPath() + (query == null ? "" : "?" + query);
            return new Connection(url, decode(credentials[0]), decode(credentials[1]));
        } catch (IllegalArgumentException ex) {
            // No adjuntar la excepción original: puede contener la URL con contraseña.
            throw new IllegalStateException("URL PostgreSQL de Neon inválida; revisa las variables DATABASE_URL");
        }
    }

    private static String decode(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    record Connection(String url, String username, String password) {
        @Override public String toString() { return "Connection[credentials=redacted]"; }
    }
}
