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
            mail = rs.getString("mail"),
            nombre = rs.getString("nombre"),
            apellido = rs.getString("apellido"),
            password = rs.getString("password"),
            token = rs.getString("token")
        )
    }

    fun save(usuario: Usuario): Usuario {
        val sql = """
            INSERT INTO usuarios (mail, nombre, apellido, password, token)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id
        """
        val id = jdbcTemplate.queryForObject(sql, arrayOf(
            usuario.mail,
            usuario.nombre,
            usuario.apellido,
            usuario.password,
            usuario.token
        ), Long::class.java)
        return usuario.copy(id = id ?: throw IllegalStateException("No se pudo guardar el usuario"))
    }

    fun findByMail(mail: String): Usuario? {
        val sql = "SELECT * FROM usuarios WHERE mail = ?"
        return jdbcTemplate.query(sql, usuarioRowMapper, mail).firstOrNull()
    }

    fun updateToken(mail: String, token: String) {
        val sql = "UPDATE usuarios SET token = ? WHERE mail = ?"
        jdbcTemplate.update(sql, token, mail)
    }
}