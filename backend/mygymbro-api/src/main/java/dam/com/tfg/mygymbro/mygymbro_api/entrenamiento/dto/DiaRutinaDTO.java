package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.dto;

import java.util.List;

public record DiaRutinaDTO(
        int id,
        String nombre,
        int orden,
        List<EjercicioRutinaDTO> ejercicios
) {}