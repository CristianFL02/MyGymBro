package dam.com.tfg.mygymbro.mygymbro_api.vinculacion;

import dam.com.tfg.mygymbro.mygymbro_api.autenticacion.AuthDtos.UsuarioRespuesta;

import java.util.List;

import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/vinculacion")
public class VinculacionController {
    private final VinculacionService vinculacionService;

    public VinculacionController(VinculacionService vinculacionService) {
        this.vinculacionService = vinculacionService;
    }

    @PostMapping
    public ResponseEntity<VinculacionResponse> solicitar(
            @AuthenticationPrincipal UsuarioRespuesta entrenador,
            @RequestParam("clienteId") @Positive Long clienteId,
            @RequestParam("tipo") TipoVinculacion tipo) {
        VinculacionResponse respuesta = vinculacionService.solicitar(
                entrenador.id(), clienteId, tipo);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping("/pendientes")
    public List<VinculacionResponse> listarPendientes(@AuthenticationPrincipal UsuarioRespuesta usuario) {
        return vinculacionService.listarPendientes(usuario.id());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VinculacionResponse> obtenerPorId(
            @PathVariable int id, @AuthenticationPrincipal UsuarioRespuesta usuario) {
        return ResponseEntity.ok(vinculacionService.obtenerPorId(id, usuario.id()));
    }

    @PatchMapping("/{id}/aceptar")
    public ResponseEntity<Void> aceptar(
            @PathVariable int id, @AuthenticationPrincipal UsuarioRespuesta usuario) {
        vinculacionService.aceptar(id, usuario.id());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/rechazar")
    public ResponseEntity<Void> rechazar(
            @PathVariable int id, @AuthenticationPrincipal UsuarioRespuesta usuario) {
        vinculacionService.rechazar(id, usuario.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/clientes")
    public List<ClienteVinculadoResponse> listarClientes(@AuthenticationPrincipal UsuarioRespuesta entrenador) {
        return vinculacionService.listarClientes(entrenador.id());
    }
}
