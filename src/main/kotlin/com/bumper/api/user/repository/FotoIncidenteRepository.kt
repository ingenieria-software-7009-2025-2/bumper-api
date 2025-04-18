package com.bumper.api.user.repository

import com.bumper.api.user.domain.FotoIncidente
import com.bumper.api.user.domain.Incidente
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import javax.sql.DataSource
import java.time.LocalDateTime

@Repository
class FotoIncidenteRepository(
    private val dataSource: DataSource,
    private val incidenteRepository: IncidenteRepository
) {
    private val jdbcTemplate = JdbcTemplate(dataSource)

    // Definición del RowMapper para FotoIncidente
    private val fotoIncidenteRowMapper = RowMapper { rs, _ ->
        val incidenteId = rs.getString("incidente_id")
        val incidente = incidenteRepository.findById(incidenteId)
            ?: throw IllegalStateException("Incidente no encontrado para la foto")

        FotoIncidente(
            id = rs.getLong("id"),
            incidente = incidente,
            urlFoto = rs.getString("url_foto"),
            descripcion = rs.getString("descripcion"),
            fechaSubida = rs.getTimestamp("fecha_subida").toLocalDateTime()
        )
    }

    /**
     * Guarda una nueva foto de incidente
     */
    fun save(fotoIncidente: FotoIncidente): Long {
        val sql = """
            INSERT INTO fotos_incidentes (incidente_id, url_foto, descripcion, fecha_subida)
            VALUES (?, ?, ?, ?)
            RETURNING id
        """
        return jdbcTemplate.queryForObject(sql, Long::class.java,
            fotoIncidente.incidente.id,
            fotoIncidente.urlFoto,
            fotoIncidente.descripcion,
            java.sql.Timestamp.valueOf(fotoIncidente.fechaSubida)
        ) ?: throw IllegalStateException("No se pudo guardar la foto del incidente")
    }

    /**
     * Encuentra todas las fotos asociadas a un incidente
     */
    fun findByIncidenteId(incidenteId: String): List<FotoIncidente> {
        val sql = "SELECT * FROM fotos_incidentes WHERE incidente_id = ? ORDER BY fecha_subida"
        return jdbcTemplate.query(sql, fotoIncidenteRowMapper, incidenteId)
    }

    /**
     * Elimina una foto específica
     */
    fun delete(id: Long) {
        val sql = "DELETE FROM fotos_incidentes WHERE id = ?"
        jdbcTemplate.update(sql, id)
    }

    /**
     * Elimina todas las fotos de un incidente
     */
    fun deleteByIncidenteId(incidenteId: String) {
        val sql = "DELETE FROM fotos_incidentes WHERE incidente_id = ?"
        jdbcTemplate.update(sql, incidenteId)
    }

    /**
     * Cuenta el número de fotos de un incidente
     */
    fun countFotosByIncidenteId(incidenteId: String): Int {
        val sql = "SELECT COUNT(*) FROM fotos_incidentes WHERE incidente_id = ?"
        return jdbcTemplate.queryForObject(sql, Int::class.java, incidenteId) ?: 0
    }

    /**
     * Actualiza la descripción de una foto
     */
    fun updateDescripcion(id: Long, descripcion: String) {
        val sql = "UPDATE fotos_incidentes SET descripcion = ? WHERE id = ?"
        jdbcTemplate.update(sql, descripcion, id)
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
        val count = jdbcTemplate.queryForObject(sql, Int::class.java, fotoId, usuarioId) ?: 0
        return count > 0
    }
}