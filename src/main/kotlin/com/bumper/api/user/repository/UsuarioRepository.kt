package com.bumper.api.user.repository

import com.bumper.api.user.domain.Usuario
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import javax.sql.DataSource
import java.time.LocalDateTime


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
        logger.info("Guardando usuario: ${usuario.correo}")

        val sql = """
            INSERT INTO usuarios (
                nombre, apellido, correo, password, 
                token, numero_incidentes, fecha_registro
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
        """

        try {
            // Primero insertamos el usuario
            jdbcTemplate.update(sql,
                usuario.nombre,
                usuario.apellido,
                usuario.correo,
                usuario.password,
                usuario.token,
                usuario.numeroIncidentes,
                Timestamp.valueOf(usuario.fechaRegistro)
            )

            // Luego obtenemos el usuario recién creado por su correo
            return findByCorreo(usuario.correo)
                ?: throw IllegalStateException("No se pudo recuperar el usuario guardado")

        } catch (e: Exception) {
            logger.error("Error al guardar usuario: ${e.message}", e)
            throw IllegalStateException("Error al guardar el usuario: ${e.message}")
        }
    }

    fun findByCorreo(correo: String): Usuario? {
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
            jdbcTemplate.query(sql, { rs, _ ->
                Usuario(
                    id = rs.getLong("id"),
                    nombre = rs.getString("nombre"),
                    apellido = rs.getString("apellido"),
                    correo = rs.getString("correo"),
                    password = rs.getString("password"),
                    token = rs.getString("token") ?: Usuario.TOKEN_INACTIVO,
                    numeroIncidentes = rs.getInt("numero_incidentes"),
                    fechaRegistro = rs.getTimestamp("fecha_registro")?.toLocalDateTime() ?: LocalDateTime.now()
                )
            }, id).firstOrNull()
        } catch (e: Exception) {
            logger.error("Error al buscar usuario por ID $id: ${e.message}", e)
            null
        }
    }

    fun findByIds(ids: List<Long>): List<Usuario> {
        if (ids.isEmpty()) return emptyList()
        val placeholders = ids.joinToString(",") { "?" }
        val sql = "SELECT * FROM usuarios WHERE id IN ($placeholders)"
        return try {
            val args = ids.toTypedArray()
            jdbcTemplate.query(sql, { rs, _ ->
                Usuario(
                    id = rs.getLong("id"),
                    nombre = rs.getString("nombre"),
                    apellido = rs.getString("apellido"),
                    correo = rs.getString("correo"),
                    password = rs.getString("password"),
                    token = rs.getString("token") ?: Usuario.TOKEN_INACTIVO,
                    numeroIncidentes = rs.getInt("numero_incidentes"),
                    fechaRegistro = rs.getTimestamp("fecha_registro")?.toLocalDateTime() ?: LocalDateTime.now()
                )
            }, *args)
        } catch (e: Exception) {
            logger.error("Error al buscar usuarios por IDs $ids: ${e.message}", e)
            emptyList()
        }
    }

    @Transactional
    fun updateToken(correo: String, nuevoToken: String): Boolean {
        logger.info("Actualizando token para usuario: $correo")
        val sql = "UPDATE usuarios SET token = ? WHERE correo = ?"
        return try {
            val rowsAffected = jdbcTemplate.update(sql, nuevoToken, correo)
            rowsAffected > 0
        } catch (e: Exception) {
            logger.error("Error al actualizar token para usuario $correo: ${e.message}", e)
            false
        }
    }

    fun existsById(id: Long): Boolean {
        val sql = "SELECT COUNT(*) FROM usuarios WHERE id = ?"
        return try {
            val count = jdbcTemplate.queryForObject(sql, Int::class.java, id) ?: 0
            count > 0
        } catch (e: Exception) {
            logger.error("Error al verificar existencia del usuario $id: ${e.message}", e)
            false
        }
    }

    @Transactional
    fun updateDatosBasicos(id: Long, nombre: String, apellido: String, password: String?): Boolean {
        logger.info("Actualizando nombre, apellido y (opcionalmente) contraseña para usuario ID: $id")
        val sql: String
        val params: Array<Any?>
        if (!password.isNullOrBlank()) {
            sql = "UPDATE usuarios SET nombre = ?, apellido = ?, password = ? WHERE id = ?"
            params = arrayOf(nombre, apellido, password, id)
        } else {
            sql = "UPDATE usuarios SET nombre = ?, apellido = ? WHERE id = ?"
            params = arrayOf(nombre, apellido, id)
        }
        return try {
            val rowsAffected = jdbcTemplate.update(sql, *params)
            rowsAffected > 0
        } catch (e: Exception) {
            logger.error("Error al actualizar datos básicos para usuario $id: ${e.message}", e)
            false
        }
    }

    @Transactional
    fun updatePassword(id: Long, nuevaPassword: String): Boolean {
        logger.info("Actualizando contraseña para usuario ID: $id")
        val sql = "UPDATE usuarios SET password = ? WHERE id = ?"
        return try {
            val rowsAffected = jdbcTemplate.update(sql, nuevaPassword, id)
            rowsAffected > 0
        } catch (e: Exception) {
            logger.error("Error al actualizar contraseña para usuario $id: ${e.message}", e)
            false
        }
    }
}