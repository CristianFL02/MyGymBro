package dam.com.tfg.mygymbro.mygymbro_api.vinculacion;

import java.util.Set;

public record ClienteVinculadoResponse(Long clienteId, String nombre, Set<TipoVinculacion> tipos) {}
