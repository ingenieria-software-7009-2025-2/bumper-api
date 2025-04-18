package com.bumper.api.user.repository

import com.bumper.api.user.domain.FotoIncidente
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import javax.sql.DataSource
import java.time.LocalDateTime
import java.sql.Timestamp
import org.slf4j.LoggerFactory

@Repository
class FotoIncidenteRepository(
    private val dataSource: DataSource
) {
    private val jdbcTemplate = JdbcTemplate(dataSource)
    private val logger = LoggerFactory.getLogger(javaClass)

    private val fotoIncidenteRowMapper = RowMapper { rs, _ ->
        FotoIncidente(
            id = rs.getLong("id"),
            incidenteId = rs.getString("incidente_id"),
            urlFoto = rs.getString("url_foto"),
            descripcion = rs.getString("descripcion"),
            fechaSubida = rs.getTimestamp("fecha_subida").toLocalDateTime()
        )
    }

    fun save(fotoIncidente: FotoIncidente): FotoIncidente {
        val sql = """
            INSERT INTO fotos_incidentes (incidente_id, url_foto, descripcion)
            VALUES (?, ?, ?)
            RETURNING id, fecha_subida
        """

        try {
            val generatedKeys = jdbcTemplate.queryForMap(
                sql,
                fotoIncidente.incidenteId,
                fotoIncidente.urlFoto,
                fotoIncidente.descripcion
            )

            return fotoIncidente.copy(
                id = generatedKeys["id"] as Long,
                fechaSubida = (generatedKeys["fecha_subida"] as Timestamp).toLocalDateTime()
            )
        } catch (e: Exception) {
            logger.error("Error al guardar foto: ${e.message}", e)
            throw IllegalStateException("Error al guardar la foto: ${e.message}")
        }
    }

    /**
     * Encuentra todas las fotos asociadas a un incidente
     * @return Lista vacía si no hay fotos o si ocurre un error
     */
    fun findByIncidenteId(incidenteId: String): List<FotoIncidente> {
        val sql = "SELECT * FROM fotos_incidentes WHERE incidente_id = ? ORDER BY fecha_subida"
        return try {
            jdbcTemplate.query(sql, fotoIncidenteRowMapper, incidenteId)
        } catch (e: Exception) {
            logger.error("Error al buscar fotos del incidente $incidenteId: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Elimina una foto específica
     * @throws IllegalStateException si la foto no puede ser eliminada
     */
    fun delete(id: Long) {
        logger.info("Eliminando foto con ID: $id")
        val sql = "DELETE FROM fotos_incidentes WHERE id = ?"
        try {
            val rowsAffected = jdbcTemplate.update(sql, id)
            if (rowsAffected == 0) {
                throw IllegalStateException("No se encontró la foto con ID $id")
            }
            logger.info("Foto eliminada exitosamente")
        } catch (e: Exception) {
            logger.error("Error al eliminar foto $id: ${e.message}", e)
            throw IllegalStateException("Error al eliminar la foto: ${e.message}")
        }
    }

    /**
     * Elimina todas las fotos de un incidente
     * @return número de fotos eliminadas
     */
    fun deleteByIncidenteId(incidenteId: String): Int {
        logger.info("Eliminando todas las fotos del incidente: $incidenteId")
        val sql = "DELETE FROM fotos_incidentes WHERE incidente_id = ?"
        return try {
            val rowsAffected = jdbcTemplate.update(sql, incidenteId)
            logger.info("$rowsAffected fotos eliminadas del incidente $incidenteId")
            rowsAffected
        } catch (e: Exception) {
            logger.error("Error al eliminar fotos del incidente $incidenteId: ${e.message}", e)
            0
        }
    }

    /**
     * Cuenta el número de fotos de un incidente
     */
    fun countFotosByIncidenteId(incidenteId: String): Int {
        val sql = "SELECT COUNT(*) FROM fotos_incidentes WHERE incidente_id = ?"
        return try {
            jdbcTemplate.queryForObject(sql, Int::class.java, incidenteId) ?: 0
        } catch (e: Exception) {
            logger.error("Error al contar fotos del incidente $incidenteId: ${e.message}", e)
            0
        }
    }

    /**
     * Actualiza la descripción de una foto
     * @throws IllegalStateException si la foto no puede ser actualizada
     */
    fun updateDescripcion(id: Long, descripcion: String) {
        logger.info("Actualizando descripción de foto ID: $id")
        val sql = "UPDATE fotos_incidentes SET descripcion = ? WHERE id = ?"
        try {
            val rowsAffected = jdbcTemplate.update(sql, descripcion, id)
            if (rowsAffected == 0) {
                throw IllegalStateException("No se encontró la foto con ID $id")
            }
            logger.info("Descripción actualizada exitosamente")
        } catch (e: Exception) {
            logger.error("Error al actualizar descripción de foto $id: ${e.message}", e)
            throw IllegalStateException("Error al actualizar la descripción: ${e.message}")
        }
    }

    /**
     * Verifica si una foto pertenece a un usuario específico
     */
    fun belongsToUser(fotoId: Long, usuarioId: Long): Boolean {
        val sql = """
            SELECT COUNT(*) FROM fotos_incidentes f
            JOIN incidentes i ON f.incidente_id = i.id
            WHERE f.id = ? AND i.usuario_id = ?
        """
        return try {
            val count = jdbcTemplate.queryForObject(sql, Int::class.java, fotoId, usuarioId) ?: 0
            count > 0
        } catch (e: Exception) {
            logger.error("Error al verificar pertenencia de foto $fotoId: ${e.message}", e)
            false
        }
    }
}