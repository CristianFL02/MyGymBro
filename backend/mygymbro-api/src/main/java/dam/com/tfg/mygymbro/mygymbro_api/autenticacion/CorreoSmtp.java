package dam.com.tfg.mygymbro.mygymbro_api.autenticacion;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;

@Service
public class CorreoSmtp implements CorreoConfirmacion {
    private final ObjectProvider<JavaMailSender> sender;
    private final String remitente;
    private final String baseUrl;

    public CorreoSmtp(ObjectProvider<JavaMailSender> sender,
                      @Value("${mygymbro.auth.mail-from:}") String remitente,
                      @Value("${mygymbro.auth.public-base-url:}") String baseUrl) {
        this.sender = sender;
        this.remitente = remitente;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    @Override
    public void enviar(String destinatario, String token) {
        JavaMailSender mail = sender.getIfAvailable();
        if (mail == null || remitente.isBlank() || !urlValida()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El envío de correos todavía no está configurado");
        }
        var mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject("Confirma tu cuenta de MyGymBro");
        mensaje.setText("Para completar tu registro en MyGymBro, abre este enlace y pulsa Confirmar correo:\n\n"
                + baseUrl + "/api/auth/verify-email?token=" + token
                + "\n\nEste enlace tiene una duración limitada. Si ha caducado, solicita otro desde MyGymBro."
                + " Si no solicitaste esta cuenta, ignora este mensaje.");
        try {
            mail.send(mensaje);
        } catch (MailException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se ha podido enviar el correo. Inténtalo de nuevo más tarde");
        }
    }

    private boolean urlValida() {
        try {
            URI uri = URI.create(baseUrl);
            return uri.getHost() != null && uri.getUserInfo() == null && uri.getQuery() == null
                    && uri.getFragment() == null && ("https".equals(uri.getScheme())
                    || ("http".equals(uri.getScheme()) && "localhost".equals(uri.getHost())));
        } catch (IllegalArgumentException ex) { return false; }
    }
}
