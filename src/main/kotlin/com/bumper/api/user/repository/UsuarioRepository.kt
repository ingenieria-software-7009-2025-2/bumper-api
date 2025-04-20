package com.bumper.api.user.repository

import com.bumper.api.user.domain.Usuario
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.sql.DataSource

@Repository
class UsuarioRepository(private val dataSource: DataSource) {
    private val logger = LoggerFactory.getLogger(javaClass)
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

    @Transactional
    fun save(usuario: Usuario): Usuario {
        logger.info("Guardando nuevo usuario: ${usuario.correo}")

        if (findByCorreo(usuario.correo) != null) {
            throw IllegalStateException("Ya existe un usuario con el correo: ${usuario.correo}")
        }

        val sql = """
        INSERT INTO usuarios (
            nombre, apellido, correo, password, token, numero_incidentes
        ) VALUES (?, ?, ?, ?, ?, ?)
    """

        val keyHolder = GeneratedKeyHolder()
        jdbcTemplate.update({ connection ->
            val ps = connection.prepareStatement(sql, arrayOf("id"))
            ps.setString(1, usuario.nombre)
            ps.setString(2, usuario.apellido)
            ps.setString(3, usuario.correo)
            ps.setString(4, usuario.password)
            ps.setString(5, usuario.token ?: "inactivo")
            ps.setInt(6, usuario.numeroIncidentes)
            ps
        }, keyHolder)

        val id = keyHolder.key?.toLong()
            ?: throw IllegalStateException("No se pudo obtener el ID del usuario creado")

        return findById(id) ?: throw IllegalStateException("No se pudo recuperar el usuario creado")
    }

    fun findByCorreo(correo: String): Usuario? {
        logger.info("Buscando usuario por correo: $correo")
        val sql = "SELECT * FROM usuarios WHERE correo = ?"
        return try {
            jdbcTemplate.query(sql, usuarioRowMapper, correo).firstOrNull()
        } catch (e: Exception) {
            logger.error("Error al buscar usuario por correo $correo: ${e.message}", e)
            null
        }
    }

    fun findById(id: Long): Usuario? {
        val sql = "SELECT * FROM usuarios WHERE id = ?"
        return try {
            jdbcTemplate.query(sql, usuarioRowMapper, id).firstOrNull()
        } catch (e: Exception) {
            logger.error("Error al buscar usuario por ID $id: ${e.message}", e)
            null
        }
    }

    fun existsById(id: Long): Boolean {
        val sql = "SELECT COUNT(*) FROM usuarios WHERE id = ?"
        return try {
            (jdbcTemplate.queryForObject(sql, Int::class.java, id) ?: 0) > 0
        } catch (e: Exception) {
            logger.error("Error al verificar existencia de usuario $id: ${e.message}", e)
            false
        }
    }

    @Transactional
    fun updateToken(correo: String, token: String): Boolean {
        logger.info("Actualizando token para usuario: $correo")
        val sql = "UPDATE usuarios SET token = ? WHERE correo = ?"
        return try {
            val rowsAffected = jdbcTemplate.update(sql, token, correo)
            rowsAffected > 0
        } catch (e: Exception) {
            logger.error("Error al actualizar token para usuario $correo: ${e.message}", e)
            false
        }
    }

    @Transactional
    fun update(usuario: Usuario): Usuario {
        logger.info("Actualizando usuario con ID: ${usuario.id}")
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

        try {
            val rowsAffected = jdbcTemplate.update(
                sql,
                usuario.nombre,
                usuario.apellido,
                usuario.correo,
                usuario.password,
                usuario.token,
                usuario.numeroIncidentes,
                usuario.id
            )

            if (rowsAffected == 0) {
                throw IllegalStateException("No se encontró usuario con ID: ${usuario.id}")
            }

            return findById(usuario.id!!) ?: throw IllegalStateException("No se pudo recuperar el usuario actualizado")
        } catch (e: Exception) {
            logger.error("Error al actualizar usuario ${usuario.id}: ${e.message}", e)
            throw IllegalStateException("Error al actualizar usuario: ${e.message}")
        }
    }

    @Transactional
    fun incrementarIncidentes(id: Long): Boolean {
        logger.info("Incrementando número de incidentes para usuario ID: $id")
        val sql = "UPDATE usuarios SET numero_incidentes = numero_incidentes + 1 WHERE id = ?"
        return try {
            val rowsAffected = jdbcTemplate.update(sql, id)
            rowsAffected > 0
        } catch (e: Exception) {
            logger.error("Error al incrementar incidentes para usuario $id: ${e.message}", e)
            false
        }
    }
}