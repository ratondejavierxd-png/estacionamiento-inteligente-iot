-- Esquema de la base de datos del Estacionamiento Inteligente (MySQL 8.4 en AWS RDS)
-- Ejecutar con un usuario administrador (por ejemplo desde MySQL Workbench).
-- Cambiar CAMBIAR_CLAVE por una contraseña real antes de ejecutar.

CREATE DATABASE IF NOT EXISTS estacionamiento;
USE estacionamiento;

-- Usuario de la API con privilegios minimos y conexion cifrada obligatoria
CREATE USER IF NOT EXISTS 'apiuser'@'%' IDENTIFIED BY 'CAMBIAR_CLAVE' REQUIRE SSL;
GRANT SELECT, INSERT, UPDATE ON estacionamiento.* TO 'apiuser'@'%';

CREATE TABLE IF NOT EXISTS usuarios (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) UNIQUE NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  creado TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS plazas (
  id INT PRIMARY KEY,
  nombre VARCHAR(20) NOT NULL,
  estado ENUM('DISPONIBLE','OCUPADO') DEFAULT 'DISPONIBLE',
  actualizado TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lecturas (
  id INT AUTO_INCREMENT PRIMARY KEY,
  plaza_id INT NOT NULL,
  distancia_m DECIMAL(5,2) NOT NULL,
  estado ENUM('DISPONIBLE','OCUPADO') NOT NULL,
  fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (plaza_id) REFERENCES plazas(id)
);

INSERT IGNORE INTO plazas (id, nombre) VALUES (1,'Plaza 1'),(2,'Plaza 2'),(3,'Plaza 3');
