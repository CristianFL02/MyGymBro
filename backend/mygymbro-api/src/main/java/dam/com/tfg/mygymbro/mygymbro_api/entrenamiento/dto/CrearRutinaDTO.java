package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearRutinaDTO(
        @NotBlank
        @Size(max = 30)
        String nombre
)
{}
