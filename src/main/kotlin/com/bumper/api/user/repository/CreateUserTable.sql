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
    hora_incidente TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tipo_vialidad VARCHAR(50) NOT NULL CHECK (tipo_vialidad IN ('CALLE', 'AVENIDA', 'CERRADA', 'OTRO')),
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'EN_PROCESO', 'RESUELTO')),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- Tabla de Fotos de Incidentes
CREATE TABLE fotos_incidentes (
  id SERIAL PRIMARY KEY,
  incidente_id INTEGER NOT NULL,
  url_foto TEXT NOT NULL, -- URL del archivo almacenado en Supabase Storage
  descripcion TEXT,
  fecha_subida TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (incidente_id) REFERENCES incidentes(id) ON DELETE CASCADE
);