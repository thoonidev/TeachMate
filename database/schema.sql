-- Import into a NEW, EMPTY MySQL database selected by the operator. No existing data is deleted.
-- Password columns reserve space for hashes introduced in the security phase.
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
  `estado` VARCHAR(500) NOT NULL,
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
    ON DELETE CASCADE
    ON UPDATE CASCADE);
    
    
