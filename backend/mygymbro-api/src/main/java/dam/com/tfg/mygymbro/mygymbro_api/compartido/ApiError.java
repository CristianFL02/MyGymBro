package dam.com.tfg.mygymbro.mygymbro_api.compartido;

import java.util.Map;

public record ApiError(int status, String mensaje, Map<String, String> campos) {
    public ApiError(int status, String mensaje) { this(status, mensaje, Map.of()); }
}
