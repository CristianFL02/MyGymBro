ALTER TABLE usuarios ALTER COLUMN contrasena_hash DROP NOT NULL;
ALTER TABLE usuarios ADD COLUMN email_verificado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuarios ADD COLUMN google_sub VARCHAR(255) NULL;
ALTER TABLE usuarios ADD CONSTRAINT uk_usuarios_google_sub UNIQUE (google_sub);

ALTER TABLE sesiones ALTER COLUMN expira_en DROP NOT NULL;
UPDATE sesiones SET expira_en = NULL;

CREATE TABLE verificaciones_correo (
    token_hash VARCHAR(64) PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    creado_en TIMESTAMP(6) NOT NULL,
    expira_en TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_verificacion_usuario UNIQUE (usuario_id),
    CONSTRAINT fk_verificacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

