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
class IncidenteRepository(private val dataSource: DataSource) {

    private val jdbcTemplate = JdbcTemplate(dataSource)

    private val incidenteRowMapper = RowMapper { rs, _ ->
        val usuario = Usuario(
            id = rs.getLong("usuario_id"),
            nombre = rs.getString("u_nombre"),
            apellido = rs.getString("u_apellido"),
            correo = rs.getString("u_correo"),
            password = rs.getString("u_password"),
            token = rs.getString("u_token"),
            numeroIncidentes = rs.getInt("u_numero_incidentes")
        )
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
        val sql = """
            SELECT i.*, 
                   u.nombre AS u_nombre, 
                   u.apellido AS u_apellido, 
                   u.correo AS u_correo, 
                   u.password AS u_password, 
                   u.token AS u_token, 
                   u.numero_incidentes AS u_numero_incidentes
            FROM incidentes i
            JOIN usuarios u ON i.usuario_id = u.id
        """
        return jdbcTemplate.query(sql, incidenteRowMapper)
    }

    fun findByUsuarioId(usuarioId: Long): List<Incidente> {
        val sql = """
            SELECT i.*, 
                   u.nombre AS u_nombre, 
                   u.apellido AS u_apellido, 
                   u.correo AS u_correo, 
                   u.password AS u_password, 
                   u.token AS u_token, 
                   u.numero_incidentes AS u_numero_incidentes
            FROM incidentes i
            JOIN usuarios u ON i.usuario_id = u.id
            WHERE i.usuario_id = ?
        """
        return jdbcTemplate.query(sql, incidenteRowMapper, usuarioId)
    }
}