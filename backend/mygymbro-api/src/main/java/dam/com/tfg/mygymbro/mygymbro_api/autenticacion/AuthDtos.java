package dam.com.tfg.mygymbro.mygymbro_api.autenticacion;

import dam.com.tfg.mygymbro.mygymbro_api.usuario.Usuario;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.Set;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record Registro(
            @NotBlank @Size(max = 100) String nombre,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String contrasena,
            @NotEmpty @Size(max = 2) Set<@NotBlank String> roles) {
        @Override
        public String toString() {
            return "Registro[contraseña=PROTEGIDA]";
        }
    }

    public record Login(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 128) String contrasena) {
        @Override
        public String toString() {
            return "Login[contrasena=PROTEGIDA]";
        }
    }

    public record GoogleLogin(@NotBlank @Size(max = 10000) String idToken,
                              @Size(max = 2) Set<@NotBlank String> roles) {
        @Override
        public String toString() {
            return "GoogleLogin[idToken=PROTEGIDO]";
        }
    }

    public record GoogleVinculo(@NotBlank @Size(max = 10000) String idToken) {
        @Override
        public String toString() {
            return "GoogleVinculo[idToken=PROTEGIDO]";
        }
    }

    public record Confirmacion(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token) {
        @Override
        public String toString() {
            return "Confirmacion[token=PROTEGIDO]";
        }
    }

    public record Reenvio(@NotBlank @Email @Size(max = 254) String email) {
    }

    public record Mensaje(String mensaje) {
    }

    public record UsuarioRespuesta(Long id, String nombre, String email, Set<String> roles) {
        static UsuarioRespuesta desde(Usuario usuario) {
            return new UsuarioRespuesta(usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getRoles());
        }
    }

    public record SesionRespuesta(String accessToken, String tokenType, Instant expiresAt,
                                  UsuarioRespuesta usuario) {
        @Override
        public String toString() {
            return "SesionRespuesta";
        }
    }
}
