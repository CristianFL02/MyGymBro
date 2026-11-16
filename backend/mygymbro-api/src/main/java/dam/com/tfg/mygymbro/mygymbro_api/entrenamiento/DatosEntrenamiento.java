package dam.com.tfg.mygymbro.mygymbro_api.entrenamiento;

final class DatosEntrenamiento {
    private DatosEntrenamiento() {}

    static String texto(String valor, int maximo) {
        if (valor == null || valor.isBlank() || valor.strip().length() > maximo) {
            throw new IllegalArgumentException("El texto debe tener entre 1 y " + maximo + " caracteres");
        }
        return valor.strip();
    }
}
