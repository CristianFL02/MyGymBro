package dam.com.tfg.mygymbro.mygymbro_api.autenticacion;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth/verify-email")
public class ConfirmacionCorreoController {
    private final AuthService service;
    public ConfirmacionCorreoController(AuthService service) { this.service = service; }

    @GetMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> pagina(@RequestParam String token) {
        if (!AuthService.tokenValido(token)) return html(400, "El enlace no es válido. Solicita otro.");
        // Abrir el enlace no lo consume: los analizadores de correo no activan cuentas por accidente.
        return html(200, "<h1>Confirma tu correo</h1><p>Pulsa el botón para completar tu registro en MyGymBro.</p>"
                + "<form method=\"post\" action=\"/api/auth/verify-email\">"
                + "<input type=\"hidden\" name=\"token\" value=\"" + token + "\">"
                + "<button type=\"submit\">Confirmar correo</button></form>");
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> confirmar(@RequestParam String token) {
        try {
            service.confirmarCorreo(token);
            return html(200, "<h1>Correo confirmado</h1><p>Ya puedes volver a MyGymBro e iniciar sesión.</p>");
        } catch (ResponseStatusException ex) {
            return html(ex.getStatusCode().value(), "<h1>No se ha podido confirmar</h1><p>El enlace no es válido o ha caducado. Solicita otro desde MyGymBro.</p>");
        }
    }

    private ResponseEntity<String> html(int status, String contenido) {
        return ResponseEntity.status(status).contentType(MediaType.TEXT_HTML)
                .header("Cache-Control", "no-store").header("Referrer-Policy", "no-referrer")
                .header("Content-Security-Policy", "default-src 'none'; form-action 'self'; frame-ancestors 'none'")
                .body("<!doctype html><html lang=\"es\"><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                        + "<title>MyGymBro</title><body>" + contenido + "</body></html>");
    }
}
