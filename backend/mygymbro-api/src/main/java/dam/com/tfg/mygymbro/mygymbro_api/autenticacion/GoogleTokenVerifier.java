package dam.com.tfg.mygymbro.mygymbro_api.autenticacion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.util.Set;

@Service
public class GoogleTokenVerifier implements GoogleIdentityVerifier {
    private final NimbusJwtDecoder decoder;
    private final String clientId;

    @Autowired
    public GoogleTokenVerifier(@Value("${mygymbro.auth.google-client-id:}") String clientId, Clock clock) {
        this(NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build(), clientId, clock);
    }

    GoogleTokenVerifier(NimbusJwtDecoder decoder, String clientId, Clock clock) {
        this.decoder = decoder;
        this.clientId = clientId;
        var timestamp = new JwtTimestampValidator();
        timestamp.setClock(clock);
        OAuth2TokenValidator<Jwt> claims = jwt -> {
            String issuer = jwt.getClaimAsString("iss");
            boolean valid = issuer != null && Set.of("https://accounts.google.com", "accounts.google.com").contains(issuer)
                    && jwt.getAudience() != null && jwt.getAudience().contains(clientId) && jwt.getExpiresAt() != null
                    && jwt.getExpiresAt().isAfter(clock.instant())
                    && jwt.getSubject() != null && !jwt.getSubject().isBlank()
                    && jwt.getSubject().length() <= 255
                    && Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"));
            return valid ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Identidad de Google no válida", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamp, claims));
    }

    @Override
    public Identidad verificar(String idToken) {
        if (clientId.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "El acceso con Google todavía no está configurado");
        try {
            Jwt jwt = decoder.decode(idToken);
            String email = jwt.getClaimAsString("email");
            if (email == null || email.length() > 254 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
                throw new IllegalArgumentException("Email no válido");
            }
            String nombre = jwt.getClaimAsString("name");
            if (nombre == null || nombre.isBlank()) nombre = email;
            return new Identidad(jwt.getSubject(), email, nombre.substring(0, Math.min(100, nombre.length())));
        } catch (JwtException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La identificación de Google no es válida");
        }
    }
}
