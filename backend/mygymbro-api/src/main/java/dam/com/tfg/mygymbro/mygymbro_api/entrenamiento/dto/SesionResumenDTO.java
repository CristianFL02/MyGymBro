package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.dto;

import java.time.Instant;

public record SesionResumenDTO(
        int id,
        Instant fecha,
        String nombreDia,
        int duracionMin,
        Integer rutinaId
) {
}
