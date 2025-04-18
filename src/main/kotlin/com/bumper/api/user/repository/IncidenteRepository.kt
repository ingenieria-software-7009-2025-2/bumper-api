package com.bumper.api.user.repository

import com.bumper.api.user.domain.Incidente
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
class IncidenteRepository(
    private val dataSource: DataSource,
    private val usuarioRepository: UsuarioRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val jdbcTemplate = JdbcTemplate(dataSource)

    private val incidenteRowMapper = RowMapper { rs, _ ->
        try {
            val usuarioId = rs.getLong("usuario_id")
            val usuario = usuarioRepository.findById(usuarioId)

            if (usuario == null) {
                logger.warn("Usuario no encontrado para el incidente ID: ${rs.getString("id")}, usuario_id: $usuarioId")
                null // Retornará null en lugar de lanzar excepción
            } else {
                Incidente(
                    id = rs.getString("id"),
                    usuario = usuario,
                    tipoIncidente = rs.getString("tipo_incidente"),
                    ubicacion = rs.getString("ubicacion"),
                    latitud = rs.getDouble("latitud"),
                    longitud = rs.getDouble("longitud"),
                    horaIncidente = rs.getTimestamp("hora_incidente").toLocalDateTime(),
                    tipoVialidad = rs.getString("tipo_vialidad"),
                    estado = rs.getString("estado")
                )
            }
        } catch (e: Exception) {
            logger.error("Error al mapear incidente: ${e.message}", e)
            null
        }
    }

    @Transactional
    fun save(incidente: Incidente): Incidente {
        try {
            // Verificar que el usuario existe antes de intentar guardar
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
        val sql = "SELECT * FROM incidentes ORDER BY hora_incidente DESC"
        return try {
            jdbcTemplate.query(sql, incidenteRowMapper)
                .filterNotNull() // Filtra los incidentes que no se pudieron mapear
                .map { incidente ->
                    incidente.copyWithFotos(getFotosIncidente(incidente.id!!))
                }
        } catch (e: Exception) {
            logger.error("Error al obtener todos los incidentes: ${e.message}", e)
            emptyList()
        }
    }

    fun findByUsuarioId(usuarioId: Long): List<Incidente> {
        // Primero verifica que el usuario existe
        if (!usuarioRepository.existsById(usuarioId)) {
            logger.warn("Usuario no encontrado con ID: $usuarioId")
            return emptyList()
        }

        val sql = "SELECT * FROM incidentes WHERE usuario_id = ? ORDER BY hora_incidente DESC"
        return try {
            jdbcTemplate.query(sql, incidenteRowMapper, usuarioId)
                .filterNotNull()
                .map { incidente ->
                    incidente.copyWithFotos(getFotosIncidente(incidente.id!!))
                }
        } catch (e: Exception) {
            logger.error("Error al buscar incidentes por usuario ID $usuarioId: ${e.message}", e)
            emptyList()
        }
    }

    @Transactional(readOnly = true)
    fun findById(id: String): Incidente? {
        val sql = "SELECT * FROM incidentes WHERE id = ?"
        return try {
            jdbcTemplate.query(sql, incidenteRowMapper, id)
                .filterNotNull()
                .firstOrNull()
                ?.let { incidente ->
                    incidente.copyWithFotos(getFotosIncidente(id))
                }
        } catch (e: Exception) {
            logger.error("Error al buscar incidente por ID $id: ${e.message}", e)
            null
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

    fun findByEstado(estado: String): List<Incidente> {
        val sql = """
            SELECT * FROM incidentes 
            WHERE estado = ? 
            ORDER BY hora_incidente DESC
        """
        return try {
            jdbcTemplate.query(sql, incidenteRowMapper, estado)
                .filterNotNull()
                .map { incidente ->
                    incidente.copyWithFotos(getFotosIncidente(incidente.id!!))
                }
        } catch (e: Exception) {
            logger.error("Error al buscar incidentes por estado $estado: ${e.message}", e)
            emptyList()
        }
    }

    fun findNearby(latitud: Double, longitud: Double, radioKm: Double): List<Incidente> {
        // Fórmula Haversine para calcular distancia
        val sql = """
            SELECT *, 
                (6371 * acos(
                    cos(radians(?)) * cos(radians(latitud)) *
                    cos(radians(longitud) - radians(?)) +
                    sin(radians(?)) * sin(radians(latitud))
                )) as distancia
            FROM incidentes 
            HAVING distancia <= ?
            ORDER BY distancia, hora_incidente DESC
        """

        return try {
            jdbcTemplate.query(
                sql,
                incidenteRowMapper,
                latitud, longitud, latitud, radioKm
            ).filterNotNull()
                .map { incidente ->
                    incidente.copyWithFotos(getFotosIncidente(incidente.id!!))
                }
        } catch (e: Exception) {
            logger.error("Error al buscar incidentes cercanos: ${e.message}", e)
            emptyList()
        }
    }
}