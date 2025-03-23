package com.bumper.api.user.repository

import com.bumper.api.user.domain.Incidente
import com.bumper.api.user.domain.Usuario
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import javax.sql.DataSource
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class IncidenteRepository(private val dataSource: DataSource, private val usuarioRepository: UsuarioRepository) {

    private val jdbcTemplate = JdbcTemplate(dataSource)

    private val incidenteRowMapper = RowMapper { rs, _ ->
        val usuario = usuarioRepository.findById(rs.getLong("usuario_id"))
            ?: throw IllegalStateException("Usuario no encontrado para el incidente")
        Incidente(
            id = rs.getLong("id"),
            usuario = usuario,
            tipoIncidente = rs.getString("tipo_incidente"),
            ubicacion = rs.getString("ubicacion"),
            horaIncidente = rs.getTimestamp("hora_incidente").toLocalDateTime(),
            tipoVialidad = rs.getString("tipo_vialidad")
        )
    }

    fun save(incidente: Incidente): Incidente {
        val sql = """
            INSERT INTO incidentes (usuario_id, tipo_incidente, ubicacion, hora_incidente, tipo_vialidad)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id
        """
        val id = jdbcTemplate.queryForObject(sql, arrayOf(
            incidente.usuario.id,
            incidente.tipoIncidente,
            incidente.ubicacion,
            Timestamp.valueOf(incidente.horaIncidente),
            incidente.tipoVialidad
        ), Long::class.java)
        return incidente.copy(id = id ?: throw IllegalStateException("No se pudo guardar el incidente"))
    }

    fun findAll(): List<Incidente> {
        val sql = "SELECT * FROM incidentes"
        return jdbcTemplate.query(sql, incidenteRowMapper)
    }

    fun findByUsuarioId(usuarioId: Long): List<Incidente> {
        val sql = "SELECT * FROM incidentes WHERE usuario_id = ?"
        return jdbcTemplate.query(sql, incidenteRowMapper, usuarioId)
    }
}