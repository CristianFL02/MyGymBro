CREATE TABLE roles (
    codigo VARCHAR(40) PRIMARY KEY
);

INSERT INTO roles (codigo) VALUES ('CLIENTE'), ('ENTRENADOR');

CREATE TABLE usuarios (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    contrasena_hash VARCHAR(255) NOT NULL,
    CONSTRAINT uk_usuarios_email UNIQUE (email)
);

CREATE TABLE usuario_roles (
    usuario_id BIGINT NOT NULL,
    rol_codigo VARCHAR(40) NOT NULL,
    PRIMARY KEY (usuario_id, rol_codigo),
    CONSTRAINT fk_usuario_roles_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT fk_usuario_roles_rol FOREIGN KEY (rol_codigo) REFERENCES roles(codigo)
);

CREATE TABLE sesiones (
    token_hash VARCHAR(64) PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    expira_en TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_sesiones_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE INDEX idx_sesiones_expiracion ON sesiones(expira_en);
