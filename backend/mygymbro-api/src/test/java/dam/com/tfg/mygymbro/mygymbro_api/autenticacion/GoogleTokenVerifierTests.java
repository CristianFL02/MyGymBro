package dam.com.tfg.mygymbro.mygymbro_api.autenticacion;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import org.junit.jupiter.api.*;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class GoogleTokenVerifierTests {
    private static RSAKey clave;
    private static final Instant AHORA = Instant.parse("2026-10-30T10:00:00Z");
    private GoogleTokenVerifier verifier;

    @BeforeAll static void generarClave() throws Exception { clave = new RSAKeyGenerator(2048).generate(); }

    @BeforeEach void preparar() throws Exception {
        verifier = new GoogleTokenVerifier(NimbusJwtDecoder.withPublicKey(clave.toRSAPublicKey()).build(),
                "mi-cliente", Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test void aceptaIdentidadFirmadaParaNuestraAplicacion() throws Exception {
        var identidad = verifier.verificar(firmar(claims().build(), clave));
        assertThat(identidad.sub()).isEqualTo("google-123");
        assertThat(identidad.email()).isEqualTo("ana@gmail.com");
    }

    @Test void rechazaAudienciaEmisorCaducidadYCorreoSinVerificar() throws Exception {
        for (JWTClaimsSet datos : List.of(
                claims().audience("otra-aplicacion").build(),
                claims().audience(Collections.emptyList()).build(),
                claims().issuer("https://atacante.example").build(),
                claims().expirationTime(Date.from(AHORA.minusSeconds(120))).build(),
                claims().expirationTime(null).build(),
                claims().subject(null).build(),
                claims().claim("email_verified", false).build())) {
            String token = firmar(datos, clave);
            assertThatThrownBy(() -> verifier.verificar(token)).isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(401));
        }
    }

    @Test void rechazaFirmaAjenaYTokenMalformado() throws Exception {
        String ajeno = firmar(claims().build(), new RSAKeyGenerator(2048).generate());
        for (String token : List.of(ajeno, "falso", "eyJhbGciOiJub25lIn0.e30.")) {
            assertThatThrownBy(() -> verifier.verificar(token)).isInstanceOf(ResponseStatusException.class);
        }
    }

    @Test void sinClientIdNoAceptaIdentidades() throws Exception {
        var sinConfigurar = new GoogleTokenVerifier(NimbusJwtDecoder.withPublicKey(clave.toRSAPublicKey()).build(),
                "", Clock.fixed(AHORA, ZoneOffset.UTC));
        assertThatThrownBy(() -> sinConfigurar.verificar("cualquier-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(503));
    }

    private JWTClaimsSet.Builder claims() {
        return new JWTClaimsSet.Builder().issuer("https://accounts.google.com").audience("mi-cliente")
                .subject("google-123").issueTime(Date.from(AHORA.minusSeconds(60)))
                .expirationTime(Date.from(AHORA.plusSeconds(3600)))
                .claim("email", "ana@gmail.com").claim("email_verified", true).claim("name", "Ana");
    }

    private String firmar(JWTClaimsSet datos, RSAKey key) throws Exception {
        var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), datos);
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }
}
