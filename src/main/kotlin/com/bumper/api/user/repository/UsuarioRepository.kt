package com.bumper.api

import com.bumper.api.domain.Usuario
// Utilizaremos JdbcTemplate para interactuar con la base de datos
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import javax.sql.DataSource

@Repository
class UsuarioRepository(private val dataSource: DataSource) {

    private val jdbcTemplate = JdbcTemplate(dataSource)

    // Mapeador de filas para convertir ResultSet en un objeto Usuario
    private val usuarioRowMapper = RowMapper { rs, _ ->
        Usuario(
            id = rs.getLong("id"),
            nombre = rs.getString("nombre"),
            apellido = rs.getString("apellido"),
            mail = rs.getString("mail"),
            password = rs.getString("password"),
            token = rs.getString("token")
        )
    }

    // Guardar un usuario en la base de datos
    fun save(usuario: Usuario): Usuario {
        val sql = """
            INSERT INTO usuarios (nombre, apellido, mail, password, token)
            VALUES (%s, %s, %s, %s, %s)
            RETURNING id
        """
        val id = jdbcTemplate.queryForObject(
            sql,
            arrayOf(
                usuario.nombre,
                usuario.apellido,
                usuario.mail,
                usuario.password,
                usuario.token
            ),
            Long::class.java
        )
        return usuario.copy(id = id ?: throw IllegalStateException("No se pudo guardar el usuario"))
    }

    // Buscar un usuario por su correo electrónico
    fun findByMail(mail: String): Usuario? {
        val sql = "SELECT * FROM usuarios WHERE mail = %s"
        return jdbcTemplate.query(sql, usuarioRowMapper, mail).firstOrNull()
    }

    // Actualizar el token de un usuario
    fun updateToken(mail: String, token: String) {
        val sql = "UPDATE usuarios SET token = ? WHERE mail = %s"
        jdbcTemplate.update(sql, token, mail)
    }
}