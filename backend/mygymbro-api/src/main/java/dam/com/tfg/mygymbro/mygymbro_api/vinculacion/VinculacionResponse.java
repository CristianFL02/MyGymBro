package dam.com.tfg.mygymbro.mygymbro_api.vinculacion;

import java.util.Date;

public record VinculacionResponse(
        int id,
        TipoVinculacion tipoVinculacion,
        EstadoVinculacion estadoVinculacion,
        Date fechaSolicitud,
        Long entrenadorId,
        Long clienteId
) {
    public static VinculacionResponse from(Vinculacion vinculacion) {
        return new VinculacionResponse(
                vinculacion.getId(),
                vinculacion.getTipoVinculacion(),
                vinculacion.getEstadoVinculacion(),
                vinculacion.getFechaSolicitud(),
                vinculacion.getEntrenador().getId(),
                vinculacion.getCliente().getId()
        );
    }
}
