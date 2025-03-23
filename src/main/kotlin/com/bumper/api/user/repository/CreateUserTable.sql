-- Tabla de Usuarios
CREATE TABLE usuarios (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    apellido VARCHAR(50) NOT NULL,
    correo VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    token VARCHAR(20) DEFAULT 'inactivo',
    numero_incidentes INTEGER DEFAULT 0 CHECK (numero_incidentes >= 0)
);

-- Tabla de Incidentes
CREATE TABLE incidentes (
    id SERIAL PRIMARY KEY,
    usuario_id INTEGER NOT NULL,
    tipo_incidente VARCHAR(20) NOT NULL CHECK (tipo_incidente IN ('ILUMINACION', 'BACHES', 'BASURA', 'OTRO')),
    ubicacion VARCHAR(200) NOT NULL,
    hora_incidente TIMESTAMP NOT NULL,
    tipo_vialidad VARCHAR(50) NOT NULL CHECK (tipo_vialidad IN ('CALLE', 'AVENIDA', 'BOULEVARD', 'OTRO')),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);