package com.bumper.api.user.repository

import com.bumper.api.user.domain.Usuario
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import javax.sql.DataSource

@Repository
class UsuarioRepository(private val dataSource: DataSource) {

    private val jdbcTemplate = JdbcTemplate(dataSource)

    private val usuarioRowMapper = RowMapper { rs, _ ->
        Usuario(
            id = rs.getLong("id"),
            nombre = rs.getString("nombre"),
            apellido = rs.getString("apellido"),
            correo = rs.getString("correo"),
            password = rs.getString("password"),
            token = rs.getString("token"),
            numeroIncidentes = rs.getInt("numero_incidentes")
        )
    }

    fun save(usuario: Usuario): Usuario {
        val sql = """
            INSERT INTO usuarios (nombre, apellido, correo, password, token, numero_incidentes)
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id
        """
        val id = jdbcTemplate.queryForObject(sql, arrayOf(
            usuario.nombre,
            usuario.apellido,
            usuario.correo,
            usuario.password,
            usuario.token,
            usuario.numeroIncidentes
        ), Long::class.java)
        return usuario.copy(id = id ?: throw IllegalStateException("No se pudo guardar el usuario"))
    }

    fun findByCorreo(correo: String): Usuario? {
        val sql = "SELECT * FROM usuarios WHERE correo = ?"
        return jdbcTemplate.query(sql, usuarioRowMapper, correo).firstOrNull()
    }

    fun findById(id: Long): Usuario? {
        val sql = "SELECT * FROM usuarios WHERE id = ?"
        return jdbcTemplate.query(sql, usuarioRowMapper, id).firstOrNull()
    }

    fun updateToken(correo: String, token: String) {
        val sql = "UPDATE usuarios SET token = ? WHERE correo = ?"
        jdbcTemplate.update(sql, token, correo)
    }

    fun update(usuario: Usuario): Usuario {
        val sql = """
            UPDATE usuarios
            SET nombre = ?, apellido = ?, correo = ?, password = ?, token = ?, numero_incidentes = ?
            WHERE id = ?
        """
        jdbcTemplate.update(sql,
            usuario.nombre,
            usuario.apellido,
            usuario.correo,
            usuario.password,
            usuario.token,
            usuario.numeroIncidentes,
            usuario.id
        )
        return usuario
    }
}