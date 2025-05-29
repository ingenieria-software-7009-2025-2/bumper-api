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

/**
 * Repositorio de acceso a datos para la entidad Usuario
 * Implementa operaciones CRUD utilizando JdbcTemplate para interacción directa con la base de datos
 * Maneja la persistencia y recuperación de usuarios en la tabla 'usuarios'
 */
@Repository
class UsuarioRepository(private val dataSource: DataSource) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val jdbcTemplate = JdbcTemplate(dataSource)

    /**
     * RowMapper reutilizable para mapear ResultSet a entidad Usuario
     * Convierte filas de la base de datos a objetos Usuario de dominio
     * Maneja la conversión de tipos SQL a tipos Kotlin
     */
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

    /**
     * Persiste un nuevo usuario en la base de datos
     * Ejecuta INSERT y recupera el registro creado para retornar con ID generado
     * Operación transaccional que garantiza consistencia de datos
     * @param usuario Entidad Usuario a persistir (sin ID)
     * @return Usuario persistido con ID generado por la base de datos
     * @throws IllegalStateException si no se puede recuperar el usuario guardado
     */
    @Transactional
    fun save(usuario: Usuario): Usuario {
        logger.info("Guardando usuario: ${usuario.correo}")

        // Query de inserción con todos los campos excepto ID (auto-generado)
        val sql = """
            INSERT INTO usuarios (
                nombre, apellido, correo, password, 
                token, numero_incidentes, fecha_registro
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
        """

        try {
            // Ejecución del INSERT con parámetros posicionales
            jdbcTemplate.update(sql,
                usuario.nombre,
                usuario.apellido,
                usuario.correo,
                usuario.password,
                usuario.token,
                usuario.numeroIncidentes,
                Timestamp.valueOf(usuario.fechaRegistro)
            )

            // Recuperación del usuario recién insertado para obtener el ID generado
            return findByCorreo(usuario.correo)
                ?: throw IllegalStateException("No se pudo recuperar el usuario guardado")

        } catch (e: Exception) {
            // Manejo de errores de base de datos con logging detallado
            logger.error("Error al guardar usuario: ${e.message}", e)
            throw IllegalStateException("Error al guardar el usuario: ${e.message}")
        }
    }

    /**
     * Busca un usuario por su dirección de correo electrónico
     * Utiliza el RowMapper compartido para mapeo consistente
     * @param correo Dirección de correo electrónico única del usuario
     * @return Usuario encontrado o null si no existe
     */
    fun findByCorreo(correo: String): Usuario? {
        val sql = "SELECT * FROM usuarios WHERE correo = ?"

        return try {
            // Consulta con parámetro y mapeo automático usando RowMapper
            jdbcTemplate.query(sql, usuarioRowMapper, correo).firstOrNull()
        } catch (e: Exception) {
            // Logging de errores sin propagar excepción (retorna null)
            logger.error("Error al buscar usuario por correo $correo: ${e.message}", e)
            null
        }
    }

    /**
     * Busca un usuario por su identificador único
     * Implementa RowMapper inline con manejo de valores nulos
     * @param id Identificador único del usuario (clave primaria)
     * @return Usuario encontrado o null si no existe
     */
    fun findById(id: Long): Usuario? {
        val sql = "SELECT * FROM usuarios WHERE id = ?"

        return try {
            // RowMapper inline con manejo defensivo de campos nullable
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
            // Logging de errores sin propagar excepción
            logger.error("Error al buscar usuario por ID $id: ${e.message}", e)
            null
        }
    }

    /**
     * Busca múltiples usuarios por una lista de identificadores
     * Construye query dinámico con placeholders para consulta batch optimizada
     * @param ids Lista de identificadores únicos de usuarios
     * @return Lista de usuarios encontrados (puede ser menor que la lista de IDs)
     */
    fun findByIds(ids: List<Long>): List<Usuario> {
        // Validación temprana para evitar queries innecesarios
        if (ids.isEmpty()) return emptyList()

        // Construcción dinámica de placeholders para la cláusula IN
        val placeholders = ids.joinToString(",") { "?" }
        val sql = "SELECT * FROM usuarios WHERE id IN ($placeholders)"

        return try {
            // Conversión de lista a array para parámetros vararg
            val args = ids.toTypedArray()

            // Ejecución de query batch con RowMapper inline
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
            // Retorno de lista vacía en caso de error
            logger.error("Error al buscar usuarios por IDs $ids: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Actualiza únicamente el token de sesión de un usuario
     * Operación optimizada que modifica solo un campo específico
     * Transaccional para garantizar consistencia del estado de sesión
     * @param correo Identificador del usuario por correo
     * @param nuevoToken Nuevo valor del token (activo/inactivo)
     * @return true si se actualizó al menos un registro, false en caso contrario
     */
    @Transactional
    fun updateToken(correo: String, nuevoToken: String): Boolean {
        logger.info("Actualizando token para usuario: $correo")

        val sql = "UPDATE usuarios SET token = ? WHERE correo = ?"

        return try {
            // Ejecución de UPDATE y verificación de filas afectadas
            val rowsAffected = jdbcTemplate.update(sql, nuevoToken, correo)
            rowsAffected > 0
        } catch (e: Exception) {
            // Logging de errores y retorno de false para indicar fallo
            logger.error("Error al actualizar token para usuario $correo: ${e.message}", e)
            false
        }
    }

    /**
     * Verifica la existencia de un usuario por su identificador
     * Consulta optimizada que solo cuenta registros sin recuperar datos
     * @param id Identificador único del usuario
     * @return true si el usuario existe, false en caso contrario
     */
    fun existsById(id: Long): Boolean {
        val sql = "SELECT COUNT(*) FROM usuarios WHERE id = ?"

        return try {
            // Query de conteo optimizada para verificación de existencia
            val count = jdbcTemplate.queryForObject(sql, Int::class.java, id) ?: 0
            count > 0
        } catch (e: Exception) {
            // Retorno de false en caso de error de consulta
            logger.error("Error al verificar existencia del usuario $id: ${e.message}", e)
            false
        }
    }

    /**
     * Actualiza datos básicos del usuario con contraseña opcional
     * Construye query dinámico basado en si se incluye actualización de contraseña
     * Transaccional para mantener consistencia de los datos actualizados
     * @param id Identificador único del usuario
     * @param nombre Nuevo nombre del usuario
     * @param apellido Nuevo apellido del usuario
     * @param password Nueva contraseña (opcional, puede ser null)
     * @return true si se actualizó al menos un registro, false en caso contrario
     */
    @Transactional
    fun updateDatosBasicos(id: Long, nombre: String, apellido: String, password: String?): Boolean {
        logger.info("Actualizando nombre, apellido y (opcionalmente) contraseña para usuario ID: $id")

        // Construcción condicional de query y parámetros
        val sql: String
        val params: Array<Any?>

        if (!password.isNullOrBlank()) {
            // Query completo incluyendo actualización de contraseña
            sql = "UPDATE usuarios SET nombre = ?, apellido = ?, password = ? WHERE id = ?"
            params = arrayOf(nombre, apellido, password, id)
        } else {
            // Query sin actualización de contraseña
            sql = "UPDATE usuarios SET nombre = ?, apellido = ? WHERE id = ?"
            params = arrayOf(nombre, apellido, id)
        }

        return try {
            // Ejecución de UPDATE con parámetros dinámicos
            val rowsAffected = jdbcTemplate.update(sql, *params)
            rowsAffected > 0
        } catch (e: Exception) {
            // Logging de errores y retorno de false
            logger.error("Error al actualizar datos básicos para usuario $id: ${e.message}", e)
            false
        }
    }

    /**
     * Actualiza únicamente la contraseña de un usuario específico
     * Operación especializada y segura para cambio de contraseñas
     * Transaccional para garantizar atomicidad de la operación
     * @param id Identificador único del usuario
     * @param nuevaPassword Nueva contraseña en texto plano
     * @return true si se actualizó al menos un registro, false en caso contrario
     */
    @Transactional
    fun updatePassword(id: Long, nuevaPassword: String): Boolean {
        logger.info("Actualizando contraseña para usuario ID: $id")

        val sql = "UPDATE usuarios SET password = ? WHERE id = ?"

        return try {
            // Ejecución de UPDATE específico para contraseña
            val rowsAffected = jdbcTemplate.update(sql, nuevaPassword, id)
            rowsAffected > 0
        } catch (e: Exception) {
            // Logging de errores sin exponer información sensible
            logger.error("Error al actualizar contraseña para usuario $id: ${e.message}", e)
            false
        }
    }
}