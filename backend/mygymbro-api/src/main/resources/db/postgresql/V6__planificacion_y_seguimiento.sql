ALTER TABLE ejercicios ADD COLUMN descripcion_tecnica VARCHAR(2000);
ALTER TABLE rutinas ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE rutinas ADD COLUMN ultima_modificacion TIMESTAMP(6);
ALTER TABLE ejercicios_rutina ADD COLUMN orden INTEGER NOT NULL DEFAULT 0;
ALTER TABLE ejercicios_rutina ADD COLUMN descanso_seg INTEGER;
ALTER TABLE ejercicios_rutina ADD COLUMN rir INTEGER;
ALTER TABLE ejercicios_rutina ADD COLUMN rpe DOUBLE PRECISION;
ALTER TABLE ejercicios_rutina ADD COLUMN tecnica_personalizada VARCHAR(2000);
ALTER TABLE ejercicios_rutina ADD CONSTRAINT chk_plan_esfuerzo CHECK
    ((rir IS NULL OR rir >= 0) AND (rpe IS NULL OR (rpe >= 0 AND rpe <= 10)) AND (rir IS NULL OR rpe IS NULL));
ALTER TABLE ejercicios_rutina ADD CONSTRAINT chk_plan_descanso CHECK (descanso_seg IS NULL OR descanso_seg >= 0);
ALTER TABLE sesiones_entrenamiento ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE sesiones_entrenamiento ADD COLUMN ultima_modificacion TIMESTAMP(6);
ALTER TABLE sesiones_entrenamiento ADD COLUMN dia_rutina_origen_id INTEGER;
ALTER TABLE sesiones_entrenamiento ADD COLUMN nombre_dia_origen VARCHAR(255);
ALTER TABLE registros_ejercicio ADD COLUMN orden INTEGER NOT NULL DEFAULT 0;
ALTER TABLE registros_ejercicio ADD COLUMN comodidad INTEGER;
ALTER TABLE registros_ejercicio ADD COLUMN capacidad_fuerza INTEGER;
ALTER TABLE registros_ejercicio ADD COLUMN bombeo INTEGER;
ALTER TABLE registros_ejercicio ADD COLUMN molestias INTEGER;
ALTER TABLE registros_ejercicio ADD COLUMN calidad_tecnica INTEGER;
ALTER TABLE registros_ejercicio ADD COLUMN comentarios_usuario VARCHAR(2000);
ALTER TABLE registros_ejercicio ADD COLUMN series_objetivo INTEGER;
ALTER TABLE registros_ejercicio ADD COLUMN repeticiones_min INTEGER;
ALTER TABLE registros_ejercicio ADD COLUMN repeticiones_max INTEGER;
ALTER TABLE registros_ejercicio ADD COLUMN descanso_seg INTEGER;
ALTER TABLE registros_ejercicio ADD COLUMN rir INTEGER;
ALTER TABLE registros_ejercicio ADD COLUMN rpe DOUBLE PRECISION;
ALTER TABLE registros_ejercicio ADD COLUMN tecnica_original VARCHAR(2000);
ALTER TABLE registros_ejercicio ADD COLUMN tecnica_personalizada VARCHAR(2000);
ALTER TABLE registros_ejercicio ADD CONSTRAINT chk_valoraciones CHECK (
    (comodidad IS NULL OR comodidad BETWEEN 1 AND 10) AND
    (capacidad_fuerza IS NULL OR capacidad_fuerza BETWEEN 1 AND 10) AND
    (bombeo IS NULL OR bombeo BETWEEN 1 AND 10) AND
    (molestias IS NULL OR molestias BETWEEN 1 AND 10) AND
    (calidad_tecnica IS NULL OR calidad_tecnica BETWEEN 1 AND 10));
ALTER TABLE registros_ejercicio ADD CONSTRAINT chk_registro_esfuerzo CHECK
    ((rir IS NULL OR rir >= 0) AND (rpe IS NULL OR (rpe >= 0 AND rpe <= 10)) AND (rir IS NULL OR rpe IS NULL));
ALTER TABLE series_registradas ADD COLUMN tipo VARCHAR(255) NOT NULL DEFAULT 'EFECTIVA';
ALTER TABLE series_registradas ADD COLUMN rir INTEGER;
ALTER TABLE series_registradas ALTER COLUMN rpe TYPE DOUBLE PRECISION;
ALTER TABLE series_registradas ALTER COLUMN rpe DROP NOT NULL;
ALTER TABLE series_registradas ADD CONSTRAINT chk_serie_esfuerzo CHECK
    ((rir IS NULL OR rir >= 0) AND (rpe IS NULL OR (rpe >= 0 AND rpe <= 10)) AND (rir IS NULL OR rpe IS NULL));

CREATE TABLE aproximaciones_rutina (
    ejercicio_rutina_id INTEGER NOT NULL,
    posicion INTEGER NOT NULL,
    repeticiones INTEGER NOT NULL,
    carga DOUBLE PRECISION,
    descanso_seg INTEGER,
    PRIMARY KEY (ejercicio_rutina_id, posicion),
    FOREIGN KEY (ejercicio_rutina_id) REFERENCES ejercicios_rutina(id),
    CHECK (repeticiones > 0), CHECK (carga IS NULL OR carga >= 0),
    CHECK (descanso_seg IS NULL OR descanso_seg >= 0)
);
CREATE TABLE aproximaciones_sesion (
    registro_id INTEGER NOT NULL,
    posicion INTEGER NOT NULL,
    repeticiones INTEGER NOT NULL,
    carga DOUBLE PRECISION,
    descanso_seg INTEGER,
    PRIMARY KEY (registro_id, posicion),
    FOREIGN KEY (registro_id) REFERENCES registros_ejercicio(id),
    CHECK (repeticiones > 0), CHECK (carga IS NULL OR carga >= 0),
    CHECK (descanso_seg IS NULL OR descanso_seg >= 0)
);


