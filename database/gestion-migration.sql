-- Existing databases: apply once AFTER oferta-migration.sql, with backup and old app stopped.
-- Do not re-run after a partial error; MySQL DDL commits independently.
-- Existing accepted sessions keep their legacy state; no payment is inferred.
ALTER TABLE tutoria DROP CHECK ck_tutoria_estado;
ALTER TABLE tutoria
    MODIFY COLUMN experiencia VARCHAR(500) NOT NULL,
    ADD COLUMN nivel VARCHAR(150) NOT NULL DEFAULT '',
    ADD COLUMN creada_en DATETIME NULL,
    ADD COLUMN vence_respuesta DATETIME NULL,
    ADD COLUMN vence_pago DATETIME NULL,
    ADD COLUMN plazo_pago_minutos INT NULL,
    ADD COLUMN enlace_clase VARCHAR(1000) NOT NULL DEFAULT '',
    ADD COLUMN asistencia VARCHAR(30) NOT NULL DEFAULT '',
    ADD COLUMN cancelacion_solicitada BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN motivo_cancelacion VARCHAR(1000) NOT NULL DEFAULT '',
    ADD CONSTRAINT ck_tutoria_estado CHECK(estado IN ('Pendiente','Pendiente de pago','Confirmada','Aceptada','Rechazada','Cancelada','Completada','Vencida','Incidencia'));
CREATE INDEX idx_tutoria_vencimiento ON tutoria(estado,vence_respuesta,vence_pago);

CREATE TABLE configuracion_reservas (
    id INT NOT NULL PRIMARY KEY,
    respuesta_minutos INT NULL,
    pago_minutos INT NULL,
    antelacion_minutos INT NULL,
    CONSTRAINT ck_configuracion_unica CHECK(id=1),
    CONSTRAINT ck_configuracion_plazos CHECK(
        (respuesta_minutos IS NULL AND pago_minutos IS NULL AND antelacion_minutos IS NULL) OR
        (respuesta_minutos BETWEEN 15 AND 10080 AND pago_minutos BETWEEN 15 AND 10080
         AND antelacion_minutos BETWEEN 30 AND 43200 AND antelacion_minutos>=respuesta_minutos+pago_minutos))
);
INSERT INTO configuracion_reservas(id) VALUES(1);
CREATE TABLE tutoria_evento (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tutoria_id INT NULL,
    actor_rol VARCHAR(10) NOT NULL,
    actor_id INT NULL,
    accion VARCHAR(45) NOT NULL,
    detalle VARCHAR(1000) NOT NULL,
    ocurrido_en DATETIME NOT NULL,
    FOREIGN KEY(tutoria_id) REFERENCES tutoria(id),
    INDEX idx_evento_tutoria(tutoria_id,id)
);
CREATE TABLE tutoria_cambio (
    tutoria_id INT NOT NULL PRIMARY KEY,
    version INT NOT NULL,
    fecha DATE NOT NULL,
    hora TIME NOT NULL,
    motivo VARCHAR(1000) NOT NULL,
    estado VARCHAR(15) NOT NULL,
    acepta_alumno BOOLEAN NOT NULL DEFAULT FALSE,
    acepta_docente BOOLEAN NOT NULL DEFAULT FALSE,
    vence_en DATETIME NOT NULL,
    FOREIGN KEY(tutoria_id) REFERENCES tutoria(id),
    CONSTRAINT ck_cambio_estado CHECK(estado IN ('Pendiente','Aceptada','Rechazada','Vencida','Anulada'))
);
