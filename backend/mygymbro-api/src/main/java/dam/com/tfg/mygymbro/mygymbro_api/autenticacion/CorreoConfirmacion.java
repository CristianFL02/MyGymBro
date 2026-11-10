package dam.com.tfg.mygymbro.mygymbro_api.autenticacion;

public interface CorreoConfirmacion {
    void enviar(String destinatario, String token);
}
