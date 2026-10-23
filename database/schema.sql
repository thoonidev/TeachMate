-- Import into a NEW, EMPTY MySQL database selected by the operator. No existing data is deleted.
-- New installations only. Existing databases use tutorias-migration.sql before deploying this code.
create table user_details(
id int not null auto_increment,
nombre varchar(45) not null,
email varchar(45) not null unique,
clave varchar(255) not null,
primary key (id)
);

create table especialidad(
id int not null auto_increment,
especialidad_nombre varchar(45) not null,
primary key (id)
);

create table docente(
id int not null auto_increment,
nombre varchar(45) not null,
fnacim varchar(45) not null,
calificacion varchar(45) not null,
especialidad varchar(45) not null,
email varchar(45) not null unique,
telefono varchar(45) not null,
clave varchar(255) not null,
activo boolean not null default true,
primary key (id)
);

CREATE TABLE `tutoria` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `userId` INT NOT NULL,
  `nombre` VARCHAR(45) NOT NULL,
  `genero` VARCHAR(45) NOT NULL,
  `edad` VARCHAR(45) NOT NULL,
  `ftutoria` VARCHAR(45) NOT NULL,
  `email` VARCHAR(45) NOT NULL,
  `telefono` VARCHAR(45) NOT NULL,
  `experiencia` VARCHAR(45) NOT NULL,
  `docenteId` INT NOT NULL,
  `direccion` VARCHAR(500) NOT NULL,
  `estado` VARCHAR(20) NOT NULL DEFAULT 'Pendiente',
  `comentario` VARCHAR(500) NOT NULL DEFAULT '',
  `hora_inicio` TIME NULL,
  `duracion_minutos` SMALLINT NULL,
  CONSTRAINT `ck_tutoria_estado` CHECK (`estado` IN ('Pendiente','Aceptada','Rechazada','Cancelada','Completada')),
  CONSTRAINT `ck_tutoria_horario` CHECK (
    (`hora_inicio` IS NULL AND `duracion_minutos` IS NULL) OR
    (`hora_inicio` IS NOT NULL AND `duracion_minutos` IS NOT NULL AND
     `hora_inicio` >= '00:00:00' AND `hora_inicio` <= '23:59:59' AND `duracion_minutos` BETWEEN 15 AND 180)),
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
    
    
