package dam.com.tfg.mygymbro.mygymbro_api.autenticacion;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDtos.Mensaje registrar(@Valid @RequestBody AuthDtos.Registro datos) {
        return service.registrar(datos);
    }

    @PostMapping("/login")
    public AuthDtos.SesionRespuesta iniciarSesion(@Valid @RequestBody AuthDtos.Login datos) {
        return service.iniciarSesion(datos);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cerrarSesion(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        service.cerrarSesion(authorization.substring(7)); // Para separar el prefijo del token (Bearer | eyJhbGci)
    }

    @PostMapping("/confirm-email")
    public AuthDtos.Mensaje confirmar(@Valid @RequestBody AuthDtos.Confirmacion datos) {
        return service.confirmarCorreo(datos.token());
    }

    @PostMapping("/resend-verification")
    public AuthDtos.Mensaje reenviar(@Valid @RequestBody AuthDtos.Reenvio datos) {
        return service.reenviarConfirmacion(datos.email());
    }

    @PostMapping("/google")
    public AuthDtos.SesionRespuesta google(@Valid @RequestBody AuthDtos.GoogleLogin datos) {
        return service.iniciarConGoogle(datos);
    }

    @PostMapping("/google/link")
    public AuthDtos.Mensaje vincularGoogle(@AuthenticationPrincipal AuthDtos.UsuarioRespuesta usuario,
                                           @Valid @RequestBody AuthDtos.GoogleVinculo datos) {
        return service.vincularGoogle(usuario.id(), datos.idToken());
    }

    @GetMapping("/me")
    public AuthDtos.UsuarioRespuesta actual(@AuthenticationPrincipal AuthDtos.UsuarioRespuesta usuario) {
        return usuario;
    }
}
