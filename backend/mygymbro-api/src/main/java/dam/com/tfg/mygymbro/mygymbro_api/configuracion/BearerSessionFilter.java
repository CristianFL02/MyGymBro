package dam.com.tfg.mygymbro.mygymbro_api.configuracion;

import dam.com.tfg.mygymbro.mygymbro_api.autenticacion.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

final class BearerSessionFilter extends OncePerRequestFilter {
    private final AuthService service;

    BearerSessionFilter(AuthService service) { this.service = service; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            service.autenticar(header.substring(7)).ifPresent(usuario -> {
                var authorities = usuario.roles().stream()
                        .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol)).toList();
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(usuario, null, authorities));
                SecurityContextHolder.setContext(context);
            });
        }
        chain.doFilter(request, response);
    }
}
