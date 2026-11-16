package dam.com.tfg.mygymbro.mygymbro_api.autenticacion;

import dam.com.tfg.mygymbro.mygymbro_api.usuario.Rol;
import dam.com.tfg.mygymbro.mygymbro_api.usuario.RolRepository;
import dam.com.tfg.mygymbro.mygymbro_api.usuario.Usuario;
import dam.com.tfg.mygymbro.mygymbro_api.usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class AuthService {
    private static final Set<String> ROLES_REGISTRABLES = Set.of("CLIENTE", "ENTRENADOR");
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final SesionRepository sesiones;
    private final VerificacionCorreoRepository verificaciones;
    private final CorreoConfirmacion correo;
    private final GoogleIdentityVerifier google;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final Duration verificationTtl;
    private final SecureRandom random = new SecureRandom();
    private final String hashFicticio;

    public AuthService(UsuarioRepository usuarios, RolRepository roles, SesionRepository sesiones,
                       VerificacionCorreoRepository verificaciones, CorreoConfirmacion correo,
                       GoogleIdentityVerifier google, PasswordEncoder encoder, Clock clock,
                       @Value("${mygymbro.auth.verification-ttl:PT24H}") Duration verificationTtl) {
        if (verificationTtl.isNegative() || verificationTtl.isZero())
            throw new IllegalArgumentException("Duración no válida");
        this.usuarios = usuarios;
        this.roles = roles;
        this.sesiones = sesiones;
        this.verificaciones = verificaciones;
        this.correo = correo;
        this.google = google;
        this.encoder = encoder;
        this.clock = clock;
        this.verificationTtl = verificationTtl;
        this.hashFicticio = encoder.encode(UUID.randomUUID().toString());
    }

    public AuthDtos.Mensaje registrar(AuthDtos.Registro datos) {
        validarContrasena(datos.contrasena());
        Set<Rol> seleccionados = seleccionarRoles(datos.roles());
        String email = normalizarEmail(datos.email());
        if (usuarios.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "El correo ya está registrado. Si falta confirmarlo o solicita otro enlace");
        var usuario = new Usuario(datos.nombre().strip(), email, encoder.encode(datos.contrasena()), seleccionados);
        usuarios.saveAndFlush(usuario);
        enviarConfirmacion(usuario);
        return new AuthDtos.Mensaje("Te hemos enviado un correo para confirmar tu cuenta");
    }

    public AuthDtos.Mensaje reenviarConfirmacion(String email) {
        usuarios.findLockedByEmail(normalizarEmail(email)).filter(u -> !u.isEmailVerificado()).ifPresent(usuario -> {
            var anterior = verificaciones.findByUsuarioId(usuario.getId());
            if (anterior.isPresent() && anterior.get().getCreadoEn().plusSeconds(60).isAfter(clock.instant())) return;
            anterior.ifPresent(verificaciones::delete);
            verificaciones.flush();
            enviarConfirmacion(usuario);
        });
        return new AuthDtos.Mensaje("Si la cuenta está pendiente de confirmar, recibirás un enlace. Espera un minuto antes de pedir otro");
    }

    public AuthDtos.Mensaje confirmarCorreo(String token) {
        if (!tokenValido(token)) throw enlaceInvalido();
        var verificacion = verificaciones.findLockedByHash(hashToken(token)).orElseThrow(AuthService::enlaceInvalido);
        if (!verificacion.getExpiraEn().isAfter(clock.instant())) throw enlaceInvalido();
        verificacion.getUsuario().verificarEmail();
        verificaciones.delete(verificacion);
        return new AuthDtos.Mensaje("Correo confirmado. Ya puedes iniciar sesión en MyGymBro");
    }

    private void enviarConfirmacion(Usuario usuario) {
        String token = nuevoToken();
        Instant ahora = clock.instant();
        verificaciones.saveAndFlush(new VerificacionCorreo(hashToken(token), usuario, ahora, ahora.plus(verificationTtl)));
        // Un fallo SMTP revierte el registro; nunca se anuncia un envío que no se ha realizado.
        correo.enviar(usuario.getEmail(), token);
    }

    public AuthDtos.SesionRespuesta iniciarSesion(AuthDtos.Login datos) {
        var usuario = usuarios.findByEmail(normalizarEmail(datos.email()));
        String hash = usuario.map(Usuario::getContrasenaHash).orElse(hashFicticio);
        boolean longitudCompatible = !hash.startsWith("{bcrypt}")
                || datos.contrasena().getBytes(StandardCharsets.UTF_8).length <= 72;
        boolean coincide = longitudCompatible && encoder.matches(datos.contrasena(), hash);
        if (usuario.isEmpty() || hash == null || !coincide) throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        if (!usuario.get().isEmailVerificado()) throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Confirma tu correo antes de iniciar sesión");
        // Compatibilidad con las contraseñas de la entrega anterior: se actualizan a BCrypt al entrar.
        if (encoder.upgradeEncoding(hash) && datos.contrasena().getBytes(StandardCharsets.UTF_8).length <= 72) {
            usuario.get().actualizarHash(encoder.encode(datos.contrasena()));
        }
        return crearSesion(usuario.get());
    }

    public AuthDtos.SesionRespuesta iniciarConGoogle(AuthDtos.GoogleLogin datos) {
        var identidad = google.verificar(datos.idToken());
        var existente = usuarios.findByGoogleSub(identidad.sub());
        if (existente.isPresent()) return crearSesion(existente.get());
        String email = normalizarEmail(identidad.email());
        // No unir cuentas automáticamente solo por compartir el correo.
        if (usuarios.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Ya hay una cuenta con ese correo. Entra con tu contraseña y vincula Google desde tu cuenta");
        var usuario = new Usuario(identidad.nombre(), email, null, seleccionarRoles(datos.roles()));
        usuario.verificarEmail();
        usuario.vincularGoogle(identidad.sub());
        usuarios.saveAndFlush(usuario);
        return crearSesion(usuario);
    }

    public AuthDtos.Mensaje vincularGoogle(Long usuarioId, String idToken) {
        var identidad = google.verificar(idToken);
        var usuario = usuarios.findLockedByEmail(normalizarEmail(identidad.email()))
                .filter(u -> u.getId().equals(usuarioId) && u.isEmailVerificado())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "El correo de Google debe coincidir con el de tu cuenta confirmada"));
        var propietario = usuarios.findByGoogleSub(identidad.sub());
        if (propietario.isPresent() && !propietario.get().getId().equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta identidad de Google ya está vinculada");
        }
        usuario.vincularGoogle(identidad.sub());
        return new AuthDtos.Mensaje("Google vinculado a tu cuenta");
    }

    private AuthDtos.SesionRespuesta crearSesion(Usuario usuario) {
        String token = nuevoToken();
        sesiones.save(new Sesion(hashToken(token), usuario, null));
        return new AuthDtos.SesionRespuesta(token, "Bearer", null, AuthDtos.UsuarioRespuesta.desde(usuario));
    }

    @Transactional(readOnly = true)
    public Optional<AuthDtos.UsuarioRespuesta> autenticar(String token) {
        if (!tokenValido(token)) return Optional.empty();
        return sesiones.findById(hashToken(token))
                .filter(sesion -> sesion.getExpiraEn() == null || sesion.getExpiraEn().isAfter(clock.instant()))
                .map(sesion -> AuthDtos.UsuarioRespuesta.desde(sesion.getUsuario()));
    }

    public void cerrarSesion(String token) {
        sesiones.deleteById(hashToken(token));
    }

    private Set<Rol> seleccionarRoles(Set<String> seleccion) {
        if (seleccion == null || seleccion.isEmpty() || seleccion.stream().anyMatch(Objects::isNull)
                || !ROLES_REGISTRABLES.containsAll(seleccion)) throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Selecciona CLIENTE, ENTRENADOR o ambos");
        var encontrados = new HashSet<>(roles.findAllById(seleccion));
        if (encontrados.size() != seleccion.size()) throw new IllegalStateException("Faltan roles iniciales");
        return encontrados;
    }

    private void validarContrasena(String contrasena) {
        if (contrasena.getBytes(StandardCharsets.UTF_8).length > 72) throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "La contraseña no puede superar 72 bytes UTF-8 con BCrypt");
    }

    private String nuevoToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static boolean tokenValido(String token) {
        return token != null && token.matches("[A-Za-z0-9_-]{43}");
    }

    static String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    private static ResponseStatusException enlaceInvalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "El enlace no es válido o ha caducado. Solicita otro");
    }

    private static String normalizarEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
