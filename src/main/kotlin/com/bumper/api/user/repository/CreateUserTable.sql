-- Tabla de Usuarios
CREATE TABLE usuarios (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL CHECK (length(trim(nombre)) > 0),
    apellido VARCHAR(50) NOT NULL CHECK (length(trim(apellido)) > 0),
    correo VARCHAR(100) NOT NULL UNIQUE CHECK (correo ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    password VARCHAR(100) NOT NULL CHECK (length(password) >= 6),
    token VARCHAR(20) DEFAULT 'inactivo' CHECK (token IN ('activo', 'inactivo')),
    numero_incidentes INTEGER DEFAULT 0 CHECK (numero_incidentes >= 0),
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de Incidentes
CREATE TABLE incidentes (
    id TEXT PRIMARY KEY, -- ID personalizado generado por trigger
    usuario_id INTEGER NOT NULL,
    tipo_incidente VARCHAR(20) NOT NULL CHECK (tipo_incidente IN ('ILUMINACION', 'BACHES', 'BASURA', 'OTRO')),
    ubicacion VARCHAR(200) NOT NULL CHECK (length(trim(ubicacion)) > 0),
    latitud DOUBLE PRECISION NOT NULL CHECK (latitud BETWEEN -90 AND 90),
    longitud DOUBLE PRECISION NOT NULL CHECK (longitud BETWEEN -180 AND 180),
    hora_incidente TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tipo_vialidad VARCHAR(50) NOT NULL CHECK (tipo_vialidad IN ('CALLE', 'AVENIDA', 'CERRADA', 'OTRO')),
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'EN_PROCESO', 'RESUELTO')),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- Tabla de Fotos de Incidentes
CREATE TABLE fotos_incidentes (
    id SERIAL PRIMARY KEY,
    incidente_id TEXT NOT NULL,
    url_foto TEXT NOT NULL CHECK (url_foto ~ '^https?://'),
    descripcion TEXT,
    fecha_subida TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (incidente_id) REFERENCES incidentes(id) ON DELETE CASCADE
);

-- Trigger para actualizar numero_incidentes en usuarios
CREATE OR REPLACE FUNCTION actualizar_numero_incidentes()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
UPDATE usuarios
SET numero_incidentes = numero_incidentes + 1
WHERE id = NEW.usuario_id;
ELSIF TG_OP = 'DELETE' THEN
UPDATE usuarios
SET numero_incidentes = numero_incidentes - 1
WHERE id = OLD.usuario_id;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_actualizar_incidentes
    AFTER INSERT OR DELETE ON incidentes
FOR EACH ROW
EXECUTE FUNCTION actualizar_numero_incidentes();

-- Trigger para generar ID personalizado en incidentes
CREATE OR REPLACE FUNCTION generar_id_incidente()
RETURNS TRIGGER AS $$
DECLARE
ts TEXT;
    tipo_inc TEXT;
    tipo_vial TEXT;
BEGIN
    IF NEW.id IS NULL THEN
        ts := to_char(CURRENT_TIMESTAMP, 'YYYYMMDDHH24MISSMS');
        tipo_inc := upper(left(NEW.tipo_incidente, 3));
        tipo_vial := upper(left(NEW.tipo_vialidad, 3));
        NEW.id := ts || '_' || tipo_inc || '_' || tipo_vial;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_generar_id_incidente
    BEFORE INSERT ON incidentes
    FOR EACH ROW
    EXECUTE FUNCTION generar_id_incidente();