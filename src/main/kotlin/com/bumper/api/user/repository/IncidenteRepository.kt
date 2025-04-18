package com.bumper.api.user.repository

import com.bumper.api.user.domain.Incidente
import com.bumper.api.user.domain.FotoIncidente
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import javax.sql.DataSource
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class IncidenteRepository(
    private val dataSource: DataSource,
    private val usuarioRepository: UsuarioRepository
) {
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

    // Método para obtener las fotos de un incidente
    private fun getFotosIncidente(incidenteId: String): List<FotoIncidente> {
        val sql = """
            SELECT id, url_foto, descripcion, fecha_subida 
            FROM fotos_incidentes 
            WHERE incidente_id = ?
        """
        return jdbcTemplate.query(sql) { rs, _ ->
            FotoIncidente(
                id = rs.getLong("id"),
                incidente = findById(incidenteId)!!,
                urlFoto = rs.getString("url_foto"),
                descripcion = rs.getString("descripcion"),
                fechaSubida = rs.getTimestamp("fecha_subida").toLocalDateTime()
            )
        }
    }

    fun save(incidente: Incidente): Incidente {
        val sql = """
            INSERT INTO incidentes (
                usuario_id, tipo_incidente, ubicacion, latitud, longitud,
                hora_incidente, tipo_vialidad, estado
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """
        val id = jdbcTemplate.queryForObject(sql, String::class.java,
            incidente.usuario.id,
            incidente.tipoIncidente,
            incidente.ubicacion,
            incidente.latitud,
            incidente.longitud,
            Timestamp.valueOf(incidente.horaIncidente),
            incidente.tipoVialidad,
            incidente.estado
        ) ?: throw IllegalStateException("No se pudo guardar el incidente")

        return findById(id) ?: throw IllegalStateException("No se pudo recuperar el incidente guardado")
    }

    fun findById(id: String): Incidente? {
        val sql = "SELECT * FROM incidentes WHERE id = ?"
        val incidente = jdbcTemplate.query(sql, incidenteRowMapper, id).firstOrNull()
        return incidente?.copy(fotos = getFotosIncidente(id))
    }

    fun findAll(): List<Incidente> {
        val sql = "SELECT * FROM incidentes ORDER BY hora_incidente DESC"
        return jdbcTemplate.query(sql, incidenteRowMapper).map {
            it.copy(fotos = getFotosIncidente(it.id!!))
        }
    }

    fun findByUsuarioId(usuarioId: Long): List<Incidente> {
        val sql = "SELECT * FROM incidentes WHERE usuario_id = ? ORDER BY hora_incidente DESC"
        return jdbcTemplate.query(sql, incidenteRowMapper, usuarioId).map {
            it.copy(fotos = getFotosIncidente(it.id!!))
        }
    }

    fun updateEstado(id: String, estado: String): Incidente? {
        val sql = "UPDATE incidentes SET estado = ? WHERE id = ?"
        jdbcTemplate.update(sql, estado, id)
        return findById(id)
    }

    fun findByEstado(estado: String): List<Incidente> {
        val sql = "SELECT * FROM incidentes WHERE estado = ? ORDER BY hora_incidente DESC"
        return jdbcTemplate.query(sql, incidenteRowMapper, estado).map {
            it.copy(fotos = getFotosIncidente(it.id!!))
        }
    }

    fun findNearby(latitud: Double, longitud: Double, radioKm: Double): List<Incidente> {
        val sql = """
            SELECT * FROM incidentes 
            WHERE (
                6371 * acos(
                    cos(radians(?)) * cos(radians(latitud)) *
                    cos(radians(longitud) - radians(?)) +
                    sin(radians(?)) * sin(radians(latitud))
                )
            ) <= ?
            ORDER BY hora_incidente DESC
        """
        return jdbcTemplate.query(sql, incidenteRowMapper, latitud, longitud, latitud, radioKm)
            .map { it.copy(fotos = getFotosIncidente(it.id!!)) }
    }
}