package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento.dto;

public record EjercicioRutinaDTO(
        int id,
        int ejercicioId,
        String nombre,
        int seriesObjetivo,
        int repeticionesMin,
        int repeticionesMax,
        Integer descansoSeg,
        EntrenamientoDtos.EsfuerzoDTO esfuerzoObjetivo,
        String tecnicaOriginal,
        String tecnicaPersonalizada,
        java.util.List<EntrenamientoDtos.AproximacionDTO> aproximaciones,
        java.util.List<EntrenamientoDtos.AlternativaDTO> alternativas
) {
}
