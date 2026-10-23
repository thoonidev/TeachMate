-- Apply once AFTER pagos-migration.sql, with backup and the application stopped.
-- Existing payments and sessions are preserved. No historical teacher transfer is inferred.
CREATE TABLE configuracion_liquidaciones (
    id INT NOT NULL PRIMARY KEY,
    corte_dia INT NULL,
    pago_dia INT NULL,
    CONSTRAINT ck_liquidacion_config CHECK(id=1),
    CONSTRAINT ck_liquidacion_dias CHECK((corte_dia IS NULL AND pago_dia IS NULL) OR
        (corte_dia BETWEEN 1 AND 7 AND pago_dia BETWEEN 1 AND 7 AND corte_dia<>pago_dia))
);
INSERT INTO configuracion_liquidaciones(id) VALUES(1);
CREATE TABLE liquidacion (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    docente_id INT NOT NULL,
    docente_nombre VARCHAR(45) NOT NULL,
    corte DATE NOT NULL,
    pago_previsto DATE NOT NULL,
    creada_en DATETIME NOT NULL,
    estado VARCHAR(15) NOT NULL DEFAULT 'Pendiente',
    version INT NOT NULL DEFAULT 1,
    total DECIMAL(14,2) NOT NULL,
    motivo VARCHAR(850) NOT NULL DEFAULT '',
    transferida_en DATETIME NULL,
    registrada_en DATETIME NULL,
    referencia VARCHAR(100) NULL,
    transferencia_clave CHAR(64) NULL UNIQUE,
    destino VARCHAR(200) NULL,
    FOREIGN KEY(docente_id) REFERENCES docente(id),
    CONSTRAINT ck_liquidacion_estado CHECK(estado IN ('Pendiente','Pagada','Compensada','Anulada')),
    CONSTRAINT ck_liquidacion_total CHECK(total>=0),
    INDEX idx_liquidacion_docente(docente_id,id)
);
CREATE TABLE liquidacion_ajuste (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tutoria_id INT NOT NULL,
    devolucion_id INT NOT NULL UNIQUE,
    origen_liquidacion_id INT NULL,
    estado VARCHAR(25) NOT NULL DEFAULT 'Pendiente',
    version INT NOT NULL DEFAULT 1,
    sugerido DECIMAL(10,2) NOT NULL,
    importe DECIMAL(10,2) NULL,
    motivo VARCHAR(850) NOT NULL DEFAULT '',
    respuesta VARCHAR(850) NOT NULL DEFAULT '',
    creado_en DATETIME NOT NULL,
    aceptado_en DATETIME NULL,
    FOREIGN KEY(tutoria_id) REFERENCES tutoria(id),
    FOREIGN KEY(devolucion_id) REFERENCES pago_devolucion(id),
    FOREIGN KEY(origen_liquidacion_id) REFERENCES liquidacion(id),
    CONSTRAINT ck_ajuste_estado CHECK(estado IN ('Pendiente','Propuesto','Aceptado','En revisión','Asumido por equipo')),
    CONSTRAINT ck_ajuste_importe CHECK(sugerido<=0 AND (importe IS NULL OR importe BETWEEN sugerido AND 0))
);
CREATE TABLE liquidacion_linea (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    liquidacion_id INT NOT NULL,
    tutoria_id INT NOT NULL,
    ajuste_id INT NULL,
    tipo VARCHAR(10) NOT NULL,
    curso VARCHAR(45) NOT NULL,
    fecha_sesion DATE NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    porcentaje DECIMAL(5,2) NOT NULL,
    devuelto_corte DECIMAL(10,2) NOT NULL,
    importe DECIMAL(10,2) NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    FOREIGN KEY(liquidacion_id) REFERENCES liquidacion(id),
    FOREIGN KEY(tutoria_id) REFERENCES tutoria(id),
    FOREIGN KEY(ajuste_id) REFERENCES liquidacion_ajuste(id),
    CONSTRAINT ck_liquidacion_tipo CHECK(tipo IN ('Sesión','Ajuste')),
    INDEX idx_liquidacion_lineas(liquidacion_id,id)
);
CREATE TABLE liquidacion_sesion (
    tutoria_id INT NOT NULL PRIMARY KEY,
    liquidacion_id INT NOT NULL,
    FOREIGN KEY(tutoria_id) REFERENCES tutoria(id),
    FOREIGN KEY(liquidacion_id) REFERENCES liquidacion(id)
);
CREATE TABLE liquidacion_ajuste_asignacion (
    ajuste_id INT NOT NULL PRIMARY KEY,
    liquidacion_id INT NOT NULL,
    FOREIGN KEY(ajuste_id) REFERENCES liquidacion_ajuste(id),
    FOREIGN KEY(liquidacion_id) REFERENCES liquidacion(id)
);
CREATE TABLE liquidacion_evento (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    liquidacion_id INT NULL,
    ajuste_id INT NULL,
    docente_id INT NULL,
    actor_rol VARCHAR(10) NOT NULL,
    accion VARCHAR(45) NOT NULL,
    detalle VARCHAR(1000) NOT NULL,
    ocurrido_en DATETIME NOT NULL,
    FOREIGN KEY(liquidacion_id) REFERENCES liquidacion(id),
    FOREIGN KEY(ajuste_id) REFERENCES liquidacion_ajuste(id),
    FOREIGN KEY(docente_id) REFERENCES docente(id),
    INDEX idx_liquidacion_eventos(liquidacion_id,id)
);
-- Earlier completed sessions with refunds require an explicit decision before closing.
INSERT INTO liquidacion_ajuste(tutoria_id,devolucion_id,sugerido,creado_en)
SELECT t.id,d.id,
    ROUND((t.precio-(SELECT COALESCE(SUM(x.importe),0) FROM pago_devolucion x WHERE x.tutoria_id=t.id AND x.ejecutada_en IS NOT NULL AND x.id<=d.id))*t.porcentaje_docente/100,2)
    - ROUND((t.precio-(SELECT COALESCE(SUM(x.importe),0) FROM pago_devolucion x WHERE x.tutoria_id=t.id AND x.ejecutada_en IS NOT NULL AND x.id<d.id))*t.porcentaje_docente/100,2),
    COALESCE(d.registrada_en,d.ejecutada_en)
FROM pago_devolucion d JOIN tutoria t ON t.id=d.tutoria_id JOIN pago_alumno p ON p.tutoria_id=t.id
WHERE d.ejecutada_en IS NOT NULL AND t.estado='Completada' AND t.precio IS NOT NULL
    AND t.porcentaje_docente IS NOT NULL AND p.validado_en IS NOT NULL;
