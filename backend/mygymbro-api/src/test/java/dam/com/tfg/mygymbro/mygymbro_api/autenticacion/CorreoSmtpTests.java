package dam.com.tfg.mygymbro.mygymbro_api.autenticacion;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class CorreoSmtpTests {
    @Test void preparaCorreoConEnlaceSinContrasena() {
        var sender = mock(JavaMailSender.class);
        var factory = new DefaultListableBeanFactory();
        factory.registerSingleton("mail", sender);
        var correo = new CorreoSmtp(factory.getBeanProvider(JavaMailSender.class),
                "mygymbro@gmail.com", "https://api.example.com");
        correo.enviar("ana@example.com", "token-de-prueba");
        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertThat(captor.getValue().getTo()).containsExactly("ana@example.com");
        assertThat(captor.getValue().getText()).contains("https://api.example.com/api/auth/verify-email?token=token-de-prueba");
    }

    @Test void configuracionAusenteNoSimulaEnvio() {
        var factory = new DefaultListableBeanFactory();
        var correo = new CorreoSmtp(factory.getBeanProvider(JavaMailSender.class), "", "");
        assertThatThrownBy(() -> correo.enviar("ana@example.com", "token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(503));
    }

    @Test void falloSmtpNoSePresentaComoEnvioCorrecto() {
        var sender = mock(JavaMailSender.class);
        doThrow(new MailSendException("sin conexión")).when(sender).send(any(SimpleMailMessage.class));
        var factory = new DefaultListableBeanFactory();
        factory.registerSingleton("mail", sender);
        var correo = new CorreoSmtp(factory.getBeanProvider(JavaMailSender.class),
                "mygymbro@gmail.com", "http://localhost:8080");
        assertThatThrownBy(() -> correo.enviar("ana@example.com", "token"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
