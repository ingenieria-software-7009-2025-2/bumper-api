package com.bumper.api.user.repository

import com.bumper.api.user.domain.FotoIncidente
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import javax.sql.DataSource

@Repository
class FotoIncidenteRepository(private val dataSource: DataSource) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val jdbcTemplate = JdbcTemplate(dataSource)

    private val fotoRowMapper = RowMapper { rs, _ ->
        FotoIncidente(
            id = rs.getLong("id"),
            incidenteId = rs.getString("incidente_id"),
            urlFoto = rs.getString("url_foto"),
            descripcion = rs.getString("descripcion"),
            fechaSubida = rs.getTimestamp("fecha_subida").toLocalDateTime()
        )
    }

    @Transactional
    fun save(foto: FotoIncidente): FotoIncidente {
        val sql = """
            INSERT INTO fotos_incidentes (incidente_id, url_foto, descripcion, fecha_subida)
            VALUES (?, ?, ?, ?)
        """

        try {
            jdbcTemplate.update(sql,
                foto.incidenteId,
                foto.urlFoto,
                foto.descripcion,
                Timestamp.valueOf(foto.fechaSubida)
            )

            return findByIncidenteIdAndUrl(foto.incidenteId, foto.urlFoto)
                ?: throw IllegalStateException("No se pudo recuperar la foto guardada")
        } catch (e: Exception) {
            logger.error("Error al guardar foto: ${e.message}", e)
            throw IllegalStateException("Error al guardar la foto: ${e.message}")
        }
    }

    fun findByIncidenteId(incidenteId: String): List<FotoIncidente> {
        val sql = """
            SELECT * FROM fotos_incidentes 
            WHERE incidente_id = ?
            ORDER BY fecha_subida DESC
        """

        return try {
            jdbcTemplate.query(sql, fotoRowMapper, incidenteId)
        } catch (e: Exception) {
            logger.error("Error al buscar fotos del incidente $incidenteId: ${e.message}", e)
            emptyList()
        }
    }

    private fun findByIncidenteIdAndUrl(incidenteId: String, urlFoto: String): FotoIncidente? {
        val sql = """
            SELECT * FROM fotos_incidentes 
            WHERE incidente_id = ? AND url_foto = ?
            LIMIT 1
        """

        return try {
            jdbcTemplate.query(sql, fotoRowMapper, incidenteId, urlFoto).firstOrNull()
        } catch (e: Exception) {
            logger.error("Error al buscar foto específica: ${e.message}", e)
            null
        }
    }

    @Transactional
    fun deleteByIncidenteId(incidenteId: String): Boolean {
        val sql = "DELETE FROM fotos_incidentes WHERE incidente_id = ?"
        return try {
            val rowsAffected = jdbcTemplate.update(sql, incidenteId)
            rowsAffected > 0
        } catch (e: Exception) {
            logger.error("Error al eliminar fotos del incidente $incidenteId: ${e.message}", e)
            false
        }
    }

    fun countFotosByIncidenteId(incidenteId: String): Int {
        val sql = "SELECT COUNT(*) FROM fotos_incidentes WHERE incidente_id = ?"
        return try {
            jdbcTemplate.queryForObject(sql, Int::class.java, incidenteId) ?: 0
        } catch (e: Exception) {
            logger.error("Error al contar fotos del incidente $incidenteId: ${e.message}", e)
            0
        }
    }

    fun belongsToUser(fotoId: Long, usuarioId: Long): Boolean {
        val sql = """
            SELECT COUNT(*) FROM fotos_incidentes f
            INNER JOIN incidentes i ON f.incidente_id = i.id
            WHERE f.id = ? AND i.usuario_id = ?
        """
        return try {
            val count = jdbcTemplate.queryForObject(sql, Int::class.java, fotoId, usuarioId) ?: 0
            count > 0
        } catch (e: Exception) {
            logger.error("Error al verificar pertenencia de foto: ${e.message}", e)
            false
        }
    }

    fun deleteById(id: Long): Boolean {
        val sql = "DELETE FROM fotos_incidentes WHERE id = ?"
        return try {
            val rowsAffected = jdbcTemplate.update(sql, id)
            rowsAffected > 0
        } catch (e: Exception) {
            logger.error("Error al eliminar foto $id: ${e.message}", e)
            false
        }
    }

    fun updateDescripcion(id: Long, descripcion: String): Boolean {
        val sql = "UPDATE fotos_incidentes SET descripcion = ? WHERE id = ?"
        return try {
            val rowsAffected = jdbcTemplate.update(sql, descripcion, id)
            rowsAffected > 0
        } catch (e: Exception) {
            logger.error("Error al actualizar descripción de foto $id: ${e.message}", e)
            false
        }
    }

}