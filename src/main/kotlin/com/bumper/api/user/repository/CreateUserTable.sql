CREATE TABLE usuarios (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    correo VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    token VARCHAR(100) DEFAULT 'inactivo',
    numero_incidentes INTEGER DEFAULT 0,
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_usuarios_correo ON usuarios(correo);
CREATE INDEX idx_usuarios_token ON usuarios(token);


-- Tabla de Incidentes (ID generado por el backend)
CREATE TABLE incidentes (
    id VARCHAR(50) PRIMARY KEY,
    usuario_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    tipo_incidente VARCHAR(20) NOT NULL
        CHECK (tipo_incidente IN ('ILUMINACION', 'BACHES', 'BASURA', 'OTRO')),
    ubicacion VARCHAR(200) NOT NULL
        CHECK (length(trim(ubicacion)) > 0),
    latitud DOUBLE PRECISION NOT NULL
        CHECK (latitud BETWEEN -90 AND 90),
    longitud DOUBLE PRECISION NOT NULL
        CHECK (longitud BETWEEN -180 AND 180),
    hora_incidente TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tipo_vialidad VARCHAR(50) NOT NULL
        CHECK (tipo_vialidad IN ('CALLE', 'AVENIDA', 'CERRADA', 'OTRO')),
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
        CHECK (estado IN ('PENDIENTE', 'EN_PROCESO', 'RESUELTO')),
    fotos TEXT[] DEFAULT '{}'::text[]
        CHECK (array_length(fotos, 1) <= 3)
);

-- Indices para optimización de búsquedas
CREATE INDEX idx_incidentes_ubicacion ON incidentes(latitud, longitud);
CREATE INDEX idx_incidentes_estado ON incidentes(estado);
CREATE INDEX idx_incidentes_usuario ON incidentes(usuario_id);
CREATE INDEX idx_incidentes_fecha ON incidentes(hora_incidente DESC);
CREATE INDEX idx_incidentes_usuario_id ON incidentes(usuario_id);

-- Trigger para actualizar contador de incidentes en usuarios
CREATE OR REPLACE FUNCTION actualizar_numero_incidentes()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
UPDATE usuarios
SET numero_incidentes = numero_incidentes + 1
WHERE id = NEW.usuario_id;
ELSIF TG_OP = 'DELETE' THEN
UPDATE usuarios
SET numero_incidentes = GREATEST(numero_incidentes - 1, 0)
WHERE id = OLD.usuario_id;
END IF;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_actualizar_incidentes
    AFTER INSERT OR DELETE ON incidentes
    FOR EACH ROW
    EXECUTE FUNCTION actualizar_numero_incidentes();