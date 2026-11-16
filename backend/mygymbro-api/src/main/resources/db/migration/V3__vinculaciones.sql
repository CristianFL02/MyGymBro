CREATE TABLE vinculacion
(
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    entrenador_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    tipo_vinculacion VARCHAR(20) NOT NULL,
    estado_vinculacion VARCHAR(20) NOT NULL,
    fecha_solicitud TIMESTAMP(6) NOT NULL,

    CONSTRAINT fk_vinculacion_entrenador
        FOREIGN KEY (entrenador_id) REFERENCES usuarios(id),

    CONSTRAINT fk_vinculacion_cliente
        FOREIGN KEY (cliente_id) REFERENCES usuarios(id)
);