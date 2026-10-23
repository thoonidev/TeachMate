-- Existing TeachMate schema only. Back up and stop the application before applying ONCE.
-- MySQL DDL auto-commits: do not repeat blindly if an instruction fails.
ALTER TABLE docente ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE tutoria ADD COLUMN comentario VARCHAR(500) NOT NULL DEFAULT '';
ALTER TABLE tutoria ADD COLUMN hora_inicio TIME NULL;
ALTER TABLE tutoria ADD COLUMN duracion_minutos SMALLINT NULL;

-- In the legacy app, every value other than Pendiente was the teacher's comment.
-- Preserve that exact text, even if it happens to match a new state's name.
UPDATE tutoria
SET comentario = CASE WHEN TRIM(estado) IN ('Pendiente', '') THEN '' ELSE estado END,
    estado = CASE WHEN TRIM(estado) IN ('Pendiente', '') THEN 'Pendiente' ELSE 'Completada' END;

ALTER TABLE tutoria MODIFY estado VARCHAR(20) NOT NULL DEFAULT 'Pendiente';
ALTER TABLE tutoria ADD CONSTRAINT ck_tutoria_estado
    CHECK (estado IN ('Pendiente','Aceptada','Rechazada','Cancelada','Completada'));
ALTER TABLE tutoria ADD CONSTRAINT ck_tutoria_horario CHECK (
    (hora_inicio IS NULL AND duracion_minutos IS NULL) OR
    (hora_inicio IS NOT NULL AND duracion_minutos IS NOT NULL AND
     hora_inicio >= '00:00:00' AND hora_inicio <= '23:59:59' AND duracion_minutos BETWEEN 15 AND 180));
ALTER TABLE tutoria DROP FOREIGN KEY did;
ALTER TABLE tutoria ADD CONSTRAINT did FOREIGN KEY (docenteId)
    REFERENCES docente(id) ON DELETE RESTRICT ON UPDATE CASCADE;
