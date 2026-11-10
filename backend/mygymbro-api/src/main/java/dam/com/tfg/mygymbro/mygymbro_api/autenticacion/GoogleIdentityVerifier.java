package dam.com.tfg.mygymbro.mygymbro_api.autenticacion;

public interface GoogleIdentityVerifier {
    Identidad verificar(String idToken);
    record Identidad(String sub, String email, String nombre) {}
}
