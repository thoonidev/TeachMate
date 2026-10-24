-- Apply once AFTER gestion-migration.sql, with backup and the application stopped.
-- MySQL DDL commits independently. Historical sessions receive no invented payments.
ALTER TABLE tutoria
    ADD COLUMN pago_revision BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN cobro_medio VARCHAR(80) NULL,
    ADD COLUMN cobro_titular VARCHAR(150) NULL,
    ADD COLUMN cobro_destino VARCHAR(200) NULL,
    ADD COLUMN cobro_instrucciones VARCHAR(1000) NULL,
    ADD COLUMN plazo_correccion_minutos INT NULL;

CREATE TABLE configuracion_pagos (
    id INT NOT NULL PRIMARY KEY,
    medio VARCHAR(80) NULL,
    titular VARCHAR(150) NULL,
    destino VARCHAR(200) NULL,
    instrucciones VARCHAR(1000) NULL,
    correccion_minutos INT NULL,
    CONSTRAINT ck_pagos_config_unica CHECK(id=1),
    CONSTRAINT ck_pagos_correccion CHECK(correccion_minutos IS NULL OR correccion_minutos BETWEEN 15 AND 10080)
);
INSERT INTO configuracion_pagos(id) VALUES(1);

CREATE TABLE pago_alumno (
    tutoria_id INT NOT NULL PRIMARY KEY,
    estado VARCHAR(30) NOT NULL,
    version INT NOT NULL,
    importe DECIMAL(10,2) NOT NULL,
    enviado_en DATETIME NOT NULL,
    corregir_hasta DATETIME NULL,
    observacion VARCHAR(850) NOT NULL DEFAULT '',
    validado_en DATETIME NULL,
    FOREIGN KEY(tutoria_id) REFERENCES tutoria(id),
    CONSTRAINT ck_pago_estado CHECK(estado IN ('En revisión','Observado','Validado','Devolución pendiente','Devuelto')),
    CONSTRAINT ck_pago_importe CHECK(importe>0),
    INDEX idx_pago_revision(estado,enviado_en)
);
CREATE TABLE pago_comprobante (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tutoria_id INT NOT NULL,
    version INT NOT NULL,
    medio VARCHAR(80) NOT NULL,
    referencia VARCHAR(100) NOT NULL,
    operacion_clave CHAR(64) NOT NULL,
    operado_en DATETIME NOT NULL,
    registrado_en DATETIME NOT NULL,
    actor_rol VARCHAR(10) NOT NULL,
    actor_id INT NULL,
    archivo LONGBLOB NOT NULL,
    mime VARCHAR(30) NOT NULL,
    huella CHAR(64) NOT NULL,
    FOREIGN KEY(tutoria_id) REFERENCES pago_alumno(tutoria_id),
    UNIQUE KEY uk_comprobante_version(tutoria_id,version),
    INDEX idx_comprobante_operacion(operacion_clave),
    CONSTRAINT ck_comprobante_mime CHECK(mime IN ('image/jpeg','image/png','application/pdf'))
);
CREATE TABLE pago_identificador (
    tipo VARCHAR(10) NOT NULL,
    clave CHAR(64) NOT NULL,
    tutoria_id INT NOT NULL,
    PRIMARY KEY(tipo,clave),
    FOREIGN KEY(tutoria_id) REFERENCES pago_alumno(tutoria_id)
);
CREATE TABLE pago_evento (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tutoria_id INT NOT NULL,
    actor_rol VARCHAR(10) NOT NULL,
    actor_id INT NULL,
    accion VARCHAR(45) NOT NULL,
    detalle VARCHAR(1000) NOT NULL,
    ocurrido_en DATETIME NOT NULL,
    FOREIGN KEY(tutoria_id) REFERENCES pago_alumno(tutoria_id),
    INDEX idx_pago_evento(tutoria_id,id)
);
CREATE TABLE pago_devolucion (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tutoria_id INT NOT NULL,
    importe DECIMAL(10,2) NOT NULL,
    motivo VARCHAR(850) NOT NULL,
    aprobada_en DATETIME NOT NULL,
    ejecutada_en DATETIME NULL,
    referencia VARCHAR(100) NULL,
    transferencia_clave CHAR(64) NULL UNIQUE,
    registrada_en DATETIME NULL,
    FOREIGN KEY(tutoria_id) REFERENCES pago_alumno(tutoria_id),
    CONSTRAINT ck_devolucion_importe CHECK(importe>0),
    INDEX idx_devolucion_pago(tutoria_id,id)
);
