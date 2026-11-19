ALTER TABLE registros_ejercicio ADD COLUMN ejercicio_propuesto_id INTEGER;
ALTER TABLE registros_ejercicio ADD CONSTRAINT fk_ejercicio_propuesto
    FOREIGN KEY (ejercicio_propuesto_id) REFERENCES ejercicios(id);

CREATE TABLE alternativas_rutina (
    ejercicio_rutina_id INTEGER NOT NULL,
    posicion INTEGER NOT NULL,
    ejercicio_id INTEGER NOT NULL,
    PRIMARY KEY (ejercicio_rutina_id, posicion),

    FOREIGN KEY (ejercicio_rutina_id) REFERENCES ejercicios_rutina(id),
    FOREIGN KEY (ejercicio_id) REFERENCES ejercicios(id)
);

CREATE TABLE alternativas_sesion (
    registro_id INTEGER NOT NULL,
    posicion INTEGER NOT NULL,
    ejercicio_id INTEGER NOT NULL,
    PRIMARY KEY (registro_id, posicion),

    FOREIGN KEY (registro_id) REFERENCES registros_ejercicio(id),
    FOREIGN KEY (ejercicio_id) REFERENCES ejercicios(id)
);

