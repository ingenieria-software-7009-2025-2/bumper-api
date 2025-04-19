package com.bumper.api.user.repository

import com.bumper.api.user.domain.Incidente
import com.bumper.api.user.domain.Usuario
import com.bumper.api.user.domain.FotoIncidente
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.sql.DataSource
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Repository
class IncidenteRepository(private val dataSource: DataSource, private val usuarioRepository: UsuarioRepository) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val jdbcTemplate = JdbcTemplate(dataSource)

    private val incidenteRowMapper = RowMapper { rs, _ ->
        Incidente(
            id = rs.getString("id"),
            usuario = Usuario(
                id = rs.getLong("usuario_id"),
                nombre = rs.getString("nombre_usuario"),
                apellido = rs.getString("apellido_usuario"),
                correo = rs.getString("correo_usuario"),
                password = rs.getString("password_usuario"),
                token = rs.getString("token_usuario") ?: "inactivo",
                numeroIncidentes = rs.getInt("numero_incidentes_usuario"),
                fechaRegistro = rs.getTimestamp("fecha_registro_usuario")?.toLocalDateTime() ?: LocalDateTime.now()
            ),
            tipoIncidente = rs.getString("tipo_incidente"),
            ubicacion = rs.getString("ubicacion"),
            latitud = rs.getDouble("latitud"),
            longitud = rs.getDouble("longitud"),
            horaIncidente = rs.getTimestamp("hora_incidente").toLocalDateTime(),
            tipoVialidad = rs.getString("tipo_vialidad"),
            estado = rs.getString("estado")
        )
    }

    /**
     * Guarda un nuevo incidente en la base de datos, generando un ID único basado en la fecha, tipo de incidente y tipo de vialidad.
     * Valida la existencia del usuario asociado y maneja errores de integridad o excepciones generales.
     * @param incidente El incidente a guardar
     * @return El incidente guardado con su ID asignado
     * @throws IllegalStateException Si el usuario no existe o no se puede guardar/recuperar el incidente
     */
    @Transactional
    fun save(incidente: Incidente): Incidente {
        try {
            if (!usuarioRepository.existsById(incidente.usuario.id)) {
                throw IllegalStateException("El usuario con ID ${incidente.usuario.id} no existe")
            }

            val idGenerado = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                    "_${incidente.tipoIncidente.take(3)}_${incidente.tipoVialidad.take(3)}"

            logger.info("Guardando incidente con ID generado: $idGenerado")

            val sql = """
            INSERT INTO incidentes (
                id, usuario_id, tipo_incidente, ubicacion, 
                latitud, longitud, tipo_vialidad, 
                estado, hora_incidente
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """

            jdbcTemplate.update(sql,
                idGenerado,
                incidente.usuario.id,
                incidente.tipoIncidente,
                incidente.ubicacion,
                incidente.latitud,
                incidente.longitud,
                incidente.tipoVialidad,
                incidente.estado,
                Timestamp.valueOf(incidente.horaIncidente)
            )

            return findById(idGenerado)
                ?: throw IllegalStateException("No se pudo recuperar el incidente guardado")
        } catch (e: DataIntegrityViolationException) {
            logger.error("Error de integridad de datos al guardar incidente: ${e.message}", e)
            throw IllegalStateException("Error al guardar el incidente: violación de restricciones de la base de datos")
        } catch (e: Exception) {
            logger.error("Error al guardar incidente: ${e.message}", e)
            throw IllegalStateException("Error al guardar el incidente: ${e.message}")
        }
    }

    fun findAll(): List<Incidente> {
        val sql = """
        SELECT 
            i.*,
            u.id as usuario_id,
            u.nombre as nombre_usuario,
            u.apellido as apellido_usuario,
            u.correo as correo_usuario,
            u.password as password_usuario,
            u.token as token_usuario,
            u.numero_incidentes as numero_incidentes_usuario,
            u.fecha_registro as fecha_registro_usuario
        FROM incidentes i
        INNER JOIN usuarios u ON i.usuario_id = u.id
        ORDER BY i.hora_incidente DESC
    """

        return try {
            jdbcTemplate.query(sql, incidenteRowMapper)
        } catch (e: Exception) {
            logger.error("Error al obtener todos los incidentes: ${e.message}", e)
            emptyList()
        }
    }


    fun findByUsuarioId(usuarioId: Long): List<Incidente> {
        val sql = """
        SELECT 
            i.*,
            u.id as usuario_id,
            u.nombre as nombre_usuario,
            u.apellido as apellido_usuario,
            u.correo as correo_usuario,
            u.password as password_usuario,
            u.token as token_usuario,
            u.numero_incidentes as numero_incidentes_usuario,
            u.fecha_registro as fecha_registro_usuario
        FROM incidentes i
        INNER JOIN usuarios u ON i.usuario_id = u.id
        WHERE i.usuario_id = ?
        ORDER BY i.hora_incidente DESC
    """

        return try {
            jdbcTemplate.query(sql, incidenteRowMapper, usuarioId)
        } catch (e: Exception) {
            logger.error("Error al buscar incidentes por usuario ID $usuarioId: ${e.message}", e)
            emptyList()
        }
    }

    private fun getFotosIncidente(incidenteId: String): List<FotoIncidente> {
        val sql = """
            SELECT id, incidente_id, url_foto, descripcion, fecha_subida 
            FROM fotos_incidentes 
            WHERE incidente_id = ?
        """
        return try {
            jdbcTemplate.query(sql, { rs, _ ->
                FotoIncidente(
                    id = rs.getLong("id"),
                    incidenteId = rs.getString("incidente_id"),
                    urlFoto = rs.getString("url_foto"),
                    descripcion = rs.getString("descripcion"),
                    fechaSubida = rs.getTimestamp("fecha_subida").toLocalDateTime()
                )
            }, incidenteId)
        } catch (e: Exception) {
            logger.error("Error al obtener fotos del incidente $incidenteId: ${e.message}", e)
            emptyList()
        }
    }

    @Transactional
    fun updateEstado(id: String, estado: String): Incidente? {
        val sql = "UPDATE incidentes SET estado = ? WHERE id = ?"
        return try {
            val rowsAffected = jdbcTemplate.update(sql, estado, id)
            if (rowsAffected > 0) {
                findById(id)
            } else {
                logger.warn("No se encontró el incidente con ID $id para actualizar estado")
                null
            }
        } catch (e: Exception) {
            logger.error("Error al actualizar estado del incidente $id: ${e.message}", e)
            null
        }
    }

    @Transactional(readOnly = true)
    fun findById(id: String): Incidente? {
        val sql = """
        SELECT 
            i.*,
            u.id as usuario_id,
            u.nombre as nombre_usuario,
            u.apellido as apellido_usuario,
            u.correo as correo_usuario,
            u.password as password_usuario,
            u.token as token_usuario,
            u.numero_incidentes as numero_incidentes_usuario,
            u.fecha_registro as fecha_registro_usuario
        FROM incidentes i
        INNER JOIN usuarios u ON i.usuario_id = u.id
        WHERE i.id = ?
    """
        return try {
            jdbcTemplate.query(sql, incidenteRowMapper, id)
                .firstOrNull()
                ?.let { incidente ->
                    incidente.copyWithFotos(getFotosIncidente(id))
                }
        } catch (e: Exception) {
            logger.error("Error al buscar incidente por ID $id: ${e.message}", e)
            null
        }
    }

    fun findByEstado(estado: String): List<Incidente> {
        val sql = """
        SELECT 
            i.*,
            u.id as usuario_id,
            u.nombre as nombre_usuario,
            u.apellido as apellido_usuario,
            u.correo as correo_usuario,
            u.password as password_usuario,
            u.token as token_usuario,
            u.numero_incidentes as numero_incidentes_usuario,
            u.fecha_registro as fecha_registro_usuario
        FROM incidentes i
        INNER JOIN usuarios u ON i.usuario_id = u.id
        WHERE i.estado = ? 
        ORDER BY i.hora_incidente DESC
    """
        return try {
            jdbcTemplate.query(sql, incidenteRowMapper, estado)
                .map { incidente ->
                    incidente.copyWithFotos(getFotosIncidente(incidente.id!!))
                }
        } catch (e: Exception) {
            logger.error("Error al buscar incidentes por estado $estado: ${e.message}", e)
            emptyList()
        }
    }


    /**
     * Busca incidentes cercanos a una ubicación geográfica usando la fórmula Haversine para calcular la distancia.
     * Filtra los resultados dentro de un radio especificado (en km) y los ordena por distancia y fecha descendente.
     * @param latitud Latitud del punto central
     * @param longitud Longitud del punto central
     * @param radioKm Radio de búsqueda en kilómetros
     * @return Lista de incidentes cercanos con sus fotos asociadas
     */
    fun findNearby(latitud: Double, longitud: Double, radioKm: Double): List<Incidente> {
        val sql = """
        SELECT 
            i.*,
            u.id as usuario_id,
            u.nombre as nombre_usuario,
            u.apellido as apellido_usuario,
            u.correo as correo_usuario,
            u.password as password_usuario,
            u.token as token_usuario,
            u.numero_incidentes as numero_incidentes_usuario,
            u.fecha_registro as fecha_registro_usuario,
            (6371 * acos(
                cos(radians(?)) * cos(radians(i.latitud)) *
                cos(radians(i.longitud) - radians(?)) +
                sin(radians(?)) * sin(radians(i.latitud))
            )) as distancia
        FROM incidentes i
        INNER JOIN usuarios u ON i.usuario_id = u.id
        HAVING distancia <= ?
        ORDER BY distancia, i.hora_incidente DESC
    """

        return try {
            jdbcTemplate.query(
                sql,
                incidenteRowMapper,
                latitud, longitud, latitud, radioKm
            ).map { incidente ->
                incidente.copyWithFotos(getFotosIncidente(incidente.id!!))
            }
        } catch (e: Exception) {
            logger.error("Error al buscar incidentes cercanos: ${e.message}", e)
            emptyList()
        }
    }
}