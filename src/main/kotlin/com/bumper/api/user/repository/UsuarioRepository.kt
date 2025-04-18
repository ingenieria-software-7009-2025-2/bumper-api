package com.bumper.api.user.repository

import com.bumper.api.user.domain.Usuario
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import javax.sql.DataSource
import java.time.LocalDateTime // Añadida esta importación
import java.sql.Timestamp // También es útil para manejar fechas con JDBC

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
            numeroIncidentes = rs.getInt("numero_incidentes"),
            fechaRegistro = rs.getTimestamp("fecha_registro").toLocalDateTime()
        )
    }

    fun save(usuario: Usuario): Usuario {
        val sql = """
            INSERT INTO usuarios (nombre, apellido, correo, password, token, numero_incidentes, fecha_registro)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """

        val id = jdbcTemplate.queryForObject(
            sql,
            Long::class.java,
            usuario.nombre,
            usuario.apellido,
            usuario.correo,
            usuario.password,
            usuario.token,
            usuario.numeroIncidentes,
            Timestamp.valueOf(LocalDateTime.now())
        ) ?: throw IllegalStateException("No se pudo obtener el ID del usuario creado")

        return usuario.copy(id = id)
    }

    fun findByCorreo(correo: String): Usuario? {
        val sql = "SELECT * FROM usuarios WHERE correo = ?"
        return try {
            jdbcTemplate.queryForObject(sql, usuarioRowMapper, correo)
        } catch (e: Exception) {
            null
        }
    }

    fun findById(id: Long): Usuario? {
        val sql = "SELECT * FROM usuarios WHERE id = ?"
        return try {
            jdbcTemplate.queryForObject(sql, usuarioRowMapper, id)
        } catch (e: Exception) {
            null
        }
    }

    fun updateToken(correo: String, token: String) {
        val sql = "UPDATE usuarios SET token = ? WHERE correo = ?"
        jdbcTemplate.update(sql, token, correo)
    }

    fun update(usuario: Usuario): Usuario {
        val sql = """
            UPDATE usuarios 
            SET nombre = ?, 
                apellido = ?, 
                correo = ?, 
                password = ?, 
                token = ?, 
                numero_incidentes = ?
            WHERE id = ?
        """

        jdbcTemplate.update(
            sql,
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

    fun incrementarIncidentes(id: Long) {
        val sql = "UPDATE usuarios SET numero_incidentes = numero_incidentes + 1 WHERE id = ?"
        jdbcTemplate.update(sql, id)
    }
}