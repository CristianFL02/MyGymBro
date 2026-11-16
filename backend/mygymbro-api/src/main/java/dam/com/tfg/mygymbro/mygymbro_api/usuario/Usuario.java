package dam.com.tfg.mygymbro.mygymbro_api.usuario;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String nombre;
    @Column(nullable = false, unique = true, length = 254)
    private String email;
    @Column(name = "contrasena_hash")
    private String contrasenaHash;
    @Column(name = "email_verificado", nullable = false)
    private boolean emailVerificado;
    @Column(name = "google_sub", unique = true)
    private String googleSub;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "usuario_roles", joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_codigo"))
    private Set<Rol> roles = new HashSet<>();

    protected Usuario() {
    }

    public Usuario(String nombre, String email, String contrasenaHash, Set<Rol> roles) {
        this.nombre = nombre;
        this.email = email;
        this.contrasenaHash = contrasenaHash;
        this.roles = new HashSet<>(roles);
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEmail() {
        return email;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public boolean isEmailVerificado() {
        return emailVerificado;
    }

    public void verificarEmail() {
        emailVerificado = true;
    }

    public void actualizarHash(String hash) {
        contrasenaHash = hash;
    }

    public void vincularGoogle(String sub) {
        googleSub = sub;
    }

    public Set<String> getRoles() {
        return roles.stream().map(Rol::getCodigo).collect(Collectors.toUnmodifiableSet());
    }
}
