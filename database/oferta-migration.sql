-- EXISTING databases only, AFTER tutorias-migration.sql. Back up and stop the old app first.
-- Run once. Do not re-run after a partial error: MySQL DDL commits independently.
-- No prices, percentages, availability or payments are invented for historical sessions.
-- Keep this migration with the application; it is not a temporary validation file.

-- Preflight: resolve duplicate catalog names before any DDL is applied.
DELIMITER $$
CREATE PROCEDURE teachmate_oferta_preflight()
BEGIN
    IF EXISTS (SELECT LOWER(TRIM(especialidad_nombre)) FROM especialidad GROUP BY LOWER(TRIM(especialidad_nombre)) HAVING COUNT(*) > 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Hay cursos duplicados en el catálogo. Resolver antes de migrar.';
    END IF;
END$$
CALL teachmate_oferta_preflight()$$
DROP PROCEDURE teachmate_oferta_preflight$$
DELIMITER ;

ALTER TABLE especialidad ADD COLUMN descripcion VARCHAR(500) NOT NULL DEFAULT '', ADD COLUMN activo BOOLEAN NOT NULL DEFAULT TRUE;
UPDATE especialidad SET especialidad_nombre=TRIM(especialidad_nombre) WHERE id>0;
CREATE UNIQUE INDEX uq_curso_nombre ON especialidad(especialidad_nombre);
ALTER TABLE docente ADD COLUMN presentacion VARCHAR(1000) NOT NULL DEFAULT '', ADD COLUMN niveles VARCHAR(150) NOT NULL DEFAULT '';
ALTER TABLE user_details ADD COLUMN telefono VARCHAR(45) NOT NULL DEFAULT '';

CREATE TABLE docente_curso (
    docente_id INT NOT NULL,
    curso_id INT NOT NULL,
    PRIMARY KEY(docente_id,curso_id),
    FOREIGN KEY(docente_id) REFERENCES docente(id),
    FOREIGN KEY(curso_id) REFERENCES especialidad(id)
);

-- Preserve legacy specialty text by creating its course if it was not in the catalog.
INSERT INTO especialidad(especialidad_nombre)
SELECT MIN(TRIM(d.especialidad)) FROM docente d
WHERE TRIM(d.especialidad)<>'' AND NOT EXISTS (
    SELECT 1 FROM especialidad e WHERE LOWER(TRIM(e.especialidad_nombre))=LOWER(TRIM(d.especialidad))
) GROUP BY LOWER(TRIM(d.especialidad));
INSERT INTO docente_curso(docente_id,curso_id)
SELECT d.id,e.id FROM docente d JOIN especialidad e ON LOWER(TRIM(e.especialidad_nombre))=LOWER(TRIM(d.especialidad));
ALTER TABLE docente DROP COLUMN especialidad;

CREATE TABLE tarifa_curso (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    curso_id INT NOT NULL,
    duracion_minutos SMALLINT NOT NULL,
    precio DECIMAL(9,2) NOT NULL,
    porcentaje_docente DECIMAL(5,2) NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    creada_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(curso_id) REFERENCES especialidad(id),
    CONSTRAINT ck_tarifa_duracion CHECK(duracion_minutos BETWEEN 15 AND 180),
    CONSTRAINT ck_tarifa_precio CHECK(precio>0),
    CONSTRAINT ck_tarifa_porcentaje CHECK(porcentaje_docente BETWEEN 0 AND 100),
    INDEX idx_tarifa_curso(curso_id,activa,duracion_minutos)
);
CREATE TABLE disponibilidad (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    docente_id INT NOT NULL,
    fecha DATE NOT NULL,
    hora_desde TIME NOT NULL,
    hora_hasta TIME NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    creado_por VARCHAR(45) NOT NULL,
    creada_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retirado_por VARCHAR(45) NULL,
    retirada_en TIMESTAMP NULL,
    FOREIGN KEY(docente_id) REFERENCES docente(id),
    CONSTRAINT ck_disponibilidad_hora CHECK(hora_desde>='00:00:00' AND hora_desde<hora_hasta AND hora_hasta<='23:59:00'),
    INDEX idx_disponibilidad_docente(docente_id,fecha,activa)
);
ALTER TABLE tutoria
    ADD COLUMN curso_id INT NULL,
    ADD COLUMN tarifa_id INT NULL,
    ADD COLUMN curso_nombre VARCHAR(45) NULL,
    ADD COLUMN precio DECIMAL(9,2) NULL,
    ADD COLUMN porcentaje_docente DECIMAL(5,2) NULL,
    ADD CONSTRAINT fk_tutoria_curso FOREIGN KEY(curso_id) REFERENCES especialidad(id),
    ADD CONSTRAINT fk_tutoria_tarifa FOREIGN KEY(tarifa_id) REFERENCES tarifa_curso(id);
