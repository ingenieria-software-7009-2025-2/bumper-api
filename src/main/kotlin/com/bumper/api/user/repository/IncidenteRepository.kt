package com.bumper.api.user.repository

import com.bumper.api.user.domain.Incidente
import com.bumper.api.user.domain.FotoIncidente
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import javax.sql.DataSource
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory

@Repository
class IncidenteRepository(
    private val dataSource: DataSource,
    private val usuarioRepository: UsuarioRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val jdbcTemplate = JdbcTemplate(dataSource)

    private val incidenteRowMapper = RowMapper { rs, _ ->
        val usuario = usuarioRepository.findById(rs.getLong("usuario_id"))
            ?: throw IllegalStateException("Usuario no encontrado para el incidente")

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

    fun save(incidente: Incidente): Incidente {
        val idGenerado = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                "_${incidente.tipoIncidente.take(3)}_${incidente.tipoVialidad.take(3)}"

        logger.info("Guardando incidente con ID generado: $idGenerado")

        val sql = """
            INSERT INTO incidentes (
                id, usuario_id, tipo_incidente, ubicacion, 
                latitud, longitud, tipo_vialidad, 
                estado, hora_incidente
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """

        try {
            val id = jdbcTemplate.queryForObject(
                sql,
                String::class.java,
                idGenerado,
                incidente.usuario.id,
                incidente.tipoIncidente,
                incidente.ubicacion,
                incidente.latitud,
                incidente.longitud,
                incidente.tipoVialidad,
                incidente.estado,
                Timestamp.valueOf(incidente.horaIncidente)
            ) ?: throw IllegalStateException("No se pudo obtener el ID del incidente creado")

            logger.info("Incidente guardado exitosamente con ID: $id")
            return findById(id) ?: throw IllegalStateException("No se pudo recuperar el incidente guardado")

        } catch (e: Exception) {
            logger.error("Error al guardar el incidente: ${e.message}", e)
            throw IllegalStateException("Error al guardar el incidente: ${e.message}")
        }
    }

    fun findAll(): List<Incidente> {
        val sql = "SELECT * FROM incidentes ORDER BY hora_incidente DESC"
        return try {
            jdbcTemplate.query(sql, incidenteRowMapper).map { incidente ->
                incidente.copyWithFotos(getFotosIncidente(incidente.id!!))
            }
        } catch (e: Exception) {
            logger.error("Error al obtener todos los incidentes: ${e.message}", e)
            emptyList()
        }
    }

    fun findById(id: String): Incidente? {
        val sql = "SELECT * FROM incidentes WHERE id = ?"
        return try {
            val incidente = jdbcTemplate.query(sql, incidenteRowMapper, id).firstOrNull()
            incidente?.let {
                it.copyWithFotos(getFotosIncidente(it.id!!))
            }
        } catch (e: Exception) {
            logger.error("Error al buscar incidente por ID $id: ${e.message}", e)
            null
        }
    }

    fun findByUsuarioId(usuarioId: Long): List<Incidente> {
        val sql = "SELECT * FROM incidentes WHERE usuario_id = ? ORDER BY hora_incidente DESC"
        return try {
            jdbcTemplate.query(sql, incidenteRowMapper, usuarioId).map { incidente ->
                incidente.copyWithFotos(getFotosIncidente(incidente.id!!))
            }
        } catch (e: Exception) {
            logger.error("Error al buscar incidentes por usuario ID $usuarioId: ${e.message}", e)
            emptyList()
        }
    }

    fun findByEstado(estado: String): List<Incidente> {
        val sql = "SELECT * FROM incidentes WHERE estado = ? ORDER BY hora_incidente DESC"
        return try {
            jdbcTemplate.query(sql, incidenteRowMapper, estado).map { incidente ->
                incidente.copyWithFotos(getFotosIncidente(incidente.id!!))
            }
        } catch (e: Exception) {
            logger.error("Error al buscar incidentes por estado $estado: ${e.message}", e)
            emptyList()
        }
    }

    fun updateEstado(id: String, estado: String): Incidente? {
        val sql = "UPDATE incidentes SET estado = ? WHERE id = ?"
        return try {
            val rowsAffected = jdbcTemplate.update(sql, estado, id)
            if (rowsAffected > 0) {
                findById(id)
            } else {
                logger.error("No se encontró el incidente con ID $id para actualizar estado")
                null
            }
        } catch (e: Exception) {
            logger.error("Error al actualizar estado del incidente $id: ${e.message}", e)
            null
        }
    }

    fun findNearby(latitud: Double, longitud: Double, radioKm: Double): List<Incidente> {
        val sql = """
            SELECT *, 
                (6371 * acos(
                    cos(radians(?)) * cos(radians(latitud)) *
                    cos(radians(longitud) - radians(?)) +
                    sin(radians(?)) * sin(radians(latitud))
                )) as distancia
            FROM incidentes 
            WHERE (
                6371 * acos(
                    cos(radians(?)) * cos(radians(latitud)) *
                    cos(radians(longitud) - radians(?)) +
                    sin(radians(?)) * sin(radians(latitud))
                )
            ) <= ?
            ORDER BY distancia, hora_incidente DESC
        """
        return try {
            jdbcTemplate.query(
                sql,
                incidenteRowMapper,
                latitud, longitud, latitud,
                latitud, longitud, latitud,
                radioKm
            ).map { incidente ->
                incidente.copyWithFotos(getFotosIncidente(incidente.id!!))
            }
        } catch (e: Exception) {
            logger.error("Error al buscar incidentes cercanos: ${e.message}", e)
            emptyList()
        }
    }
}