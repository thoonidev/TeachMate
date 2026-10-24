-- TeachMate: instalación nueva en MySQL 8.0.16 o posterior.
-- Crear y seleccionar una base VACÍA con utf8mb4 antes de ejecutar este archivo.
-- Ejecutar una sola vez. No elimina tablas ni datos; no actualiza instalaciones existentes.
-- No crea usuarios, cursos, tarifas ni credenciales de ejemplo.

-- Alumnos, cursos y profesores.
CREATE TABLE user_details (
    id int not null auto_increment,
    nombre varchar(45) not null,
    email varchar(45) not null unique,
    telefono varchar(45) not null default '',
    clave varchar(255) not null,
    PRIMARY KEY (id)
);

CREATE TABLE especialidad (
    id int not null auto_increment,
    especialidad_nombre varchar(45) not null unique,
    descripcion varchar(500) not null default '',
    activo boolean not null default true,
    PRIMARY KEY (id)
);

CREATE TABLE docente (
    id int not null auto_increment,
    nombre varchar(45) not null,
    fnacim varchar(45) not null,
    calificacion varchar(45) not null,
    presentacion varchar(1000) not null default '',
    niveles varchar(150) not null default '',
    email varchar(45) not null unique,
    telefono varchar(45) not null,
    clave varchar(255) not null,
    activo boolean not null default true,
    PRIMARY KEY (id)
);

-- Oferta: asignaciones, tarifas y disponibilidad.
CREATE TABLE docente_curso (
    docente_id INT NOT NULL,
    curso_id INT NOT NULL,
    PRIMARY KEY(docente_id,curso_id),
    FOREIGN KEY(docente_id) REFERENCES docente(id),
    FOREIGN KEY(curso_id) REFERENCES especialidad(id)
);

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

-- Reservas: condiciones, agenda e historial.
CREATE TABLE `tutoria` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `userId` INT NOT NULL,
  `nombre` VARCHAR(45) NOT NULL,
  `genero` VARCHAR(45) NOT NULL,
  `edad` VARCHAR(45) NOT NULL,
  `ftutoria` VARCHAR(45) NOT NULL,
  `email` VARCHAR(45) NOT NULL,
  `telefono` VARCHAR(45) NOT NULL,
  `experiencia` VARCHAR(500) NOT NULL,
  `docenteId` INT NOT NULL,
  `direccion` VARCHAR(500) NOT NULL,
  `estado` VARCHAR(20) NOT NULL DEFAULT 'Pendiente',
  `comentario` VARCHAR(500) NOT NULL DEFAULT '',
  `hora_inicio` TIME NULL,
  `duracion_minutos` SMALLINT NULL,
  CONSTRAINT `ck_tutoria_estado` CHECK (`estado` IN ('Pendiente','Pendiente de pago','Confirmada','Aceptada','Rechazada','Cancelada','Completada','Vencida','Incidencia')),
  CONSTRAINT `ck_tutoria_horario` CHECK (
    (`hora_inicio` IS NULL AND `duracion_minutos` IS NULL) OR
    (`hora_inicio` IS NOT NULL AND `duracion_minutos` IS NOT NULL AND
     `hora_inicio` >= '00:00:00' AND `hora_inicio` <= '23:59:59' AND `duracion_minutos` BETWEEN 15 AND 180)),
  curso_id INT NULL,
  tarifa_id INT NULL,
  curso_nombre VARCHAR(45) NULL,
  precio DECIMAL(9,2) NULL,
  porcentaje_docente DECIMAL(5,2) NULL,
  CONSTRAINT fk_tutoria_curso FOREIGN KEY(curso_id) REFERENCES especialidad(id),
  CONSTRAINT fk_tutoria_tarifa FOREIGN KEY(tarifa_id) REFERENCES tarifa_curso(id),
  nivel VARCHAR(150) NOT NULL DEFAULT '',
  creada_en DATETIME NULL,
  vence_respuesta DATETIME NULL,
  vence_pago DATETIME NULL,
  plazo_pago_minutos INT NULL,
  enlace_clase VARCHAR(1000) NOT NULL DEFAULT '',
  asistencia VARCHAR(30) NOT NULL DEFAULT '',
  cancelacion_solicitada BOOLEAN NOT NULL DEFAULT FALSE,
  motivo_cancelacion VARCHAR(1000) NOT NULL DEFAULT '',
  pago_revision BOOLEAN NOT NULL DEFAULT FALSE,
  cobro_medio VARCHAR(80) NULL,
  cobro_titular VARCHAR(150) NULL,
  cobro_destino VARCHAR(200) NULL,
  cobro_instrucciones VARCHAR(1000) NULL,
  plazo_correccion_minutos INT NULL,
  INDEX idx_tutoria_vencimiento (estado,vence_respuesta,vence_pago),
  PRIMARY KEY (`id`),
  INDEX `uid_idx` (`userId` ASC),
  INDEX `did_idx` (`docenteId` ASC),
  CONSTRAINT `uid`
    FOREIGN KEY (`userId`)
    REFERENCES `user_details` (`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT `did`
    FOREIGN KEY (`docenteId`)
    REFERENCES `docente` (`id`)
    ON DELETE RESTRICT
    ON UPDATE CASCADE);

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

-- Pagos: configuración, comprobantes privados y devoluciones.
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

-- Liquidaciones: cierres, ajustes y transferencias a profesores.
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
