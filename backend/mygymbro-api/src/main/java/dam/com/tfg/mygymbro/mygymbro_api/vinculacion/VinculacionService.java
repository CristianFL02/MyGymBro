package dam.com.tfg.mygymbro.mygymbro_api.vinculacion;

import dam.com.tfg.mygymbro.mygymbro_api.usuario.Usuario;
import dam.com.tfg.mygymbro.mygymbro_api.usuario.UsuarioRepository;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VinculacionService {
    private final VinculacionRepository vinculacionRepository;
    private final UsuarioRepository usuarios;

    public VinculacionService(VinculacionRepository vinculacionRepository, UsuarioRepository usuarios) {
        this.vinculacionRepository = vinculacionRepository;
        this.usuarios = usuarios;
    }

    @Transactional
    public VinculacionResponse solicitar(Long entrenadorId, Long clienteId, TipoVinculacion tipo) {
        if (entrenadorId == null || clienteId == null || tipo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faltan datos de la solicitud");
        }
        if (entrenadorId.equals(clienteId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes vincularte contigo mismo");
        }

        Usuario entrenador = usuarios.findLockedById(entrenadorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El entrenador no existe"));
        if (!entrenador.getRoles().contains("ENTRENADOR")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo un entrenador puede solicitar la vinculación");
        }

        Usuario cliente = usuarios.findById(clienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El cliente no existe"));
        if (!cliente.getRoles().contains("CLIENTE")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El destinatario debe tener el rol CLIENTE");
        }

        if (vinculacionRepository.existsByEntrenadorIdAndClienteIdAndTipoVinculacionAndEstadoVinculacionIn(
                entrenadorId, clienteId, tipo, List.of(EstadoVinculacion.PENDIENTE, EstadoVinculacion.ACEPTADA))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una vinculación pendiente o aceptada de este tipo");
        }

        Vinculacion vinculacion = vinculacionRepository.save(new Vinculacion(tipo, entrenador, cliente));
        return VinculacionResponse.from(vinculacion);
    }

    @Transactional(readOnly = true)
    public List<ClienteVinculadoResponse> listarClientes(Long entrenadorId) {
        Usuario entrenador = usuarios.findById(entrenadorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El entrenador no existe"));
        if (!entrenador.getRoles().contains("ENTRENADOR")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo un entrenador puede listar sus clientes");
        }

        var clientes = new LinkedHashMap<Long, ClienteVinculadoResponse>();
        for (Vinculacion vinculacion : vinculacionRepository
                .findByEntrenadorIdAndEstadoVinculacionOrderByClienteIdAsc(entrenadorId, EstadoVinculacion.ACEPTADA)) {
            Usuario cliente = vinculacion.getCliente();
            var respuesta = clientes.computeIfAbsent(cliente.getId(), id -> new ClienteVinculadoResponse(
                    id, cliente.getNombre(), EnumSet.noneOf(TipoVinculacion.class)));
            respuesta.tipos().add(vinculacion.getTipoVinculacion());
        }
        return List.copyOf(clientes.values());
    }

    @Transactional(readOnly = true)
    public boolean validarVinculo(Long entrenadorId, Long clienteId, TipoVinculacion tipo) {
        if (entrenadorId == null || clienteId == null || tipo == null) {
            return false;
        }
        return vinculacionRepository.existsByEntrenadorIdAndClienteIdAndTipoVinculacionAndEstadoVinculacionIn(
                entrenadorId, clienteId, tipo, List.of(EstadoVinculacion.ACEPTADA));
    }

    @Transactional(readOnly = true)
    public VinculacionResponse obtenerPorId(int id, Long usuarioId) {
        Vinculacion vinculacion = vinculacionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "La vinculación no existe"));
        if (!vinculacion.getEntrenador().getId().equals(usuarioId)
                && !vinculacion.getCliente().getId().equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No participas en esta vinculación");
        }
        return VinculacionResponse.from(vinculacion);
    }

    @Transactional(readOnly = true)
    public List<VinculacionResponse> listarPendientes(Long clienteId) {
        return vinculacionRepository.findByClienteIdAndEstadoVinculacionOrderByFechaSolicitudDescIdDesc(
                        clienteId, EstadoVinculacion.PENDIENTE)
                .stream()
                .map(VinculacionResponse::from)
                .toList();
    }

    @Transactional
    public void aceptar(int id, Long clienteId) {
        obtenerPendienteDelCliente(id, clienteId).aceptar();
    }

    @Transactional
    public void rechazar(int id, Long clienteId) {
        obtenerPendienteDelCliente(id, clienteId).rechazar();
    }

    private Vinculacion obtenerPendienteDelCliente(int id, Long clienteId) {
        Vinculacion vinculacion = vinculacionRepository.findLockedById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "La vinculación no existe"));
        if (!vinculacion.getCliente().getId().equals(clienteId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el cliente destinatario puede responder");
        }
        if (vinculacion.getEstadoVinculacion() != EstadoVinculacion.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud ya se ha resuelto");
        }
        return vinculacion;
    }
}
