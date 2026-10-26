-- Fase 3: ejecutar UNA VEZ despues de 002-plazos-atencion.sql, antes del despliegue.
-- Cola persistente de avisos; no activa envios ni contiene credenciales.
USE teachmateweb;

CREATE TABLE correo_pendiente (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    clave CHAR(64) NOT NULL UNIQUE,
    tutoria_id INT NOT NULL,
    tipo VARCHAR(45) NOT NULL,
    referencia DATETIME NULL,
    destino VARCHAR(254) NOT NULL,
    rol VARCHAR(10) NOT NULL,
    asunto VARCHAR(200) NOT NULL,
    cuerpo TEXT NOT NULL,
    uuid CHAR(36) NOT NULL,
    estado VARCHAR(15) NOT NULL DEFAULT 'Pendiente',
    intentos INT NOT NULL DEFAULT 0,
    creado_en DATETIME NOT NULL,
    programado_en DATETIME NOT NULL,
    caduca_en DATETIME NOT NULL,
    primer_intento DATETIME NULL,
    incierto BOOLEAN NOT NULL DEFAULT FALSE,
    bloqueo_hasta DATETIME NULL,
    token CHAR(36) NULL,
    aceptado_en DATETIME NULL,
    ultimo_error VARCHAR(200) NOT NULL DEFAULT '',
    FOREIGN KEY(tutoria_id) REFERENCES tutoria(id),
    CONSTRAINT ck_correo_estado CHECK(estado IN ('Pendiente','Enviando','Aceptado','Fallido','Dudoso','Cancelado')),
    CONSTRAINT ck_correo_rol CHECK(rol IN ('ALUMNO','DOCENTE','ADMIN')),
    INDEX idx_correo_programado(estado,programado_en),
    INDEX idx_correo_caducidad(estado,caduca_en),
    INDEX idx_correo_aceptado(estado,aceptado_en)
);
