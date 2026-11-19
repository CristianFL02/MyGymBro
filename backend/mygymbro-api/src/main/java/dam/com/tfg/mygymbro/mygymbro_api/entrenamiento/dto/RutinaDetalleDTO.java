package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.dto;

import java.util.List;

public record RutinaDetalleDTO(
        int id,
        long version,
        String nombre,
        List<DiaRutinaDTO> dias
) {
}
