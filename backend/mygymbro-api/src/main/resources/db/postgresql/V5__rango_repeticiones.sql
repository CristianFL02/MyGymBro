ALTER TABLE ejercicios_rutina ADD COLUMN repeticiones_min INTEGER NOT NULL DEFAULT 0;
ALTER TABLE ejercicios_rutina ADD COLUMN repeticiones_max INTEGER NOT NULL DEFAULT 0;

UPDATE ejercicios_rutina
SET repeticiones_min = repeticiones_objetivo,
    repeticiones_max = repeticiones_objetivo;

ALTER TABLE ejercicios_rutina DROP COLUMN repeticiones_objetivo;
ALTER TABLE ejercicios_rutina ADD CONSTRAINT chk_rango_repeticiones
    CHECK (repeticiones_min >= 0 AND repeticiones_max >= repeticiones_min);
