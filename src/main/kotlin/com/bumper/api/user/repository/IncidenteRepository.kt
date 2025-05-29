package com.bumper.api.user.repository

import com.bumper.api.user.domain.Incidente
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.sql.DataSource
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.springframework.jdbc.core.ConnectionCallback
import org.springframework.transaction.annotation.Propagation
import org.springframework.dao.EmptyResultDataAccessException

/**
 * Repositorio de acceso a datos para la entidad Incidente
 * Maneja operaciones CRUD y consultas especializadas para incidentes viales
 * Incluye funcionalidades geoespaciales para búsquedas por proximidad
 */
@Repository
class IncidenteRepository(
    private val dataSource: DataSource,
    private val usuarioRepository: UsuarioRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val jdbcTemplate = JdbcTemplate(dataSource)

    /**
     * RowMapper especializado para mapear ResultSet a entidad Incidente
     * Maneja la conversión de arrays SQL (fotos) a listas de Kotlin
     * Convierte tipos SQL a tipos de dominio apropiados
     */
    private val incidenteRowMapper = RowMapper { rs, _ ->
        // Procesamiento del array SQL de fotos a lista de strings
        val fotosArray = rs.getArray("fotos")
        val fotosList = if (fotosArray != null) {
            (fotosArray.array as Array<Any>).map { it.toString() }
        } else {
            emptyList()
        }

        Incidente(
            id = rs.getString("id"),
            usuarioId = rs.getLong("usuario_id"),
            tipoIncidente = rs.getString("tipo_incidente"),
            ubicacion = rs.getString("ubicacion"),
            latitud = rs.getDouble("latitud"),
            longitud = rs.getDouble("longitud"),
            horaIncidente = rs.getTimestamp("hora_incidente").toLocalDateTime(),
            tipoVialidad = rs.getString("tipo_vialidad"),
            estado = rs.getString("estado"),
            fotos = fotosList
        )
    }

    /**
     * Persiste un nuevo incidente en la base de datos
     * Genera un ID único basado en timestamp y características del incidente
     * Valida la existencia del usuario antes de crear el incidente
     * @param incidente Entidad Incidente a persistir
     * @return Incidente persistido con ID generado
     * @throws IllegalStateException si el usuario no existe o hay errores de persistencia
     */
    @Transactional
    fun save(incidente: Incidente): Incidente {
        try {
            // Validación de integridad referencial con tabla usuarios
            if (!usuarioRepository.existsById(incidente.usuarioId)) {
                throw IllegalStateException("El usuario con ID ${incidente.usuarioId} no existe")
            }

            // Generación de ID único basado en timestamp y metadatos del incidente
            val idGenerado = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                    "_${incidente.tipoIncidente.take(3)}_${incidente.tipoVialidad.take(3)}"

            logger.info("Guardando incidente con ID generado: $idGenerado")

            // Query de inserción con manejo de array SQL para fotos
            val sql = """
            INSERT INTO incidentes (
                id, usuario_id, tipo_incidente, ubicacion, 
                latitud, longitud, tipo_vialidad, 
                estado, hora_incidente, fotos
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """

            // Creación de array SQL para el campo fotos usando ConnectionCallback
            val fotosArray = jdbcTemplate.execute(ConnectionCallback { connection ->
                connection.createArrayOf("text", incidente.fotos.toTypedArray())
            })

            // Ejecución del INSERT con todos los parámetros incluyendo array SQL
            jdbcTemplate.update(sql,
                idGenerado,
                incidente.usuarioId,
                incidente.tipoIncidente,
                incidente.ubicacion,
                incidente.latitud,
                incidente.longitud,
                incidente.tipoVialidad,
                incidente.estado,
                Timestamp.valueOf(incidente.horaIncidente),
                fotosArray
            )

            // Recuperación del incidente recién creado para retornar con datos completos
            return findById(idGenerado)
                ?: throw IllegalStateException("No se pudo recuperar el incidente guardado")

        } catch (e: Exception) {
            // Logging detallado de errores y propagación como IllegalStateException
            logger.error("Error al guardar incidente: ${e.message}", e)
            throw IllegalStateException("Error al guardar el incidente: ${e.message}")
        }
    }

    /**
     * Busca un incidente por su identificador único
     * Maneja específicamente EmptyResultDataAccessException para casos de no encontrado
     * @param id Identificador único del incidente
     * @return Incidente encontrado o null si no existe
     */
    fun findById(id: String): Incidente? {
        return try {
            val jdbcTemplate = JdbcTemplate(dataSource)
            val sql = "SELECT * FROM incidentes WHERE id = ?"

            // Uso de queryForObject que lanza excepción si no encuentra resultados
            jdbcTemplate.queryForObject(sql, incidenteRowMapper, id)
        } catch (e: EmptyResultDataAccessException) {
            // Manejo específico para casos de no encontrado (comportamiento esperado)
            logger.warn("No se encontró el incidente con ID $id")
            null
        } catch (e: Exception) {
            // Manejo de errores inesperados de base de datos
            logger.error("Error al buscar incidente con ID $id: ${e.message}", e)
            null
        }
    }

    /**
     * Obtiene todos los incidentes ordenados por fecha de creación descendente
     * Consulta optimizada para listados generales del sistema
     * @return Lista de todos los incidentes o lista vacía si hay errores
     */
    fun findAll(): List<Incidente> {
        val sql = """
        SELECT * FROM incidentes
        ORDER BY hora_incidente DESC
        """

        return try {
            // Consulta simple con ordenamiento por fecha para mostrar más recientes primero
            jdbcTemplate.query(sql, incidenteRowMapper)
        } catch (e: Exception) {
            // Retorno de lista vacía en caso de error para evitar fallos en UI
            logger.error("Error al obtener todos los incidentes: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Busca todos los incidentes asociados a un usuario específico
     * Consulta filtrada por usuario con ordenamiento temporal
     * @param usuarioId Identificador del usuario propietario de los incidentes
     * @return Lista de incidentes del usuario o lista vacía si hay errores
     */
    fun findByUsuarioId(usuarioId: Long): List<Incidente> {
        val sql = """
        SELECT * FROM incidentes
        WHERE usuario_id = ?
        ORDER BY hora_incidente DESC
        """

        return try {
            // Consulta filtrada con parámetro de usuario
            jdbcTemplate.query(sql, incidenteRowMapper, usuarioId)
        } catch (e: Exception) {
            // Logging específico con contexto del usuario
            logger.error("Error al buscar incidentes por usuario ID $usuarioId: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Busca incidentes por su estado actual
     * Útil para filtros de dashboard y reportes por estado
     * @param estado Estado del incidente (PENDIENTE, EN_PROCESO, RESUELTO)
     * @return Lista de incidentes con el estado especificado
     */
    fun findByEstado(estado: String): List<Incidente> {
        val sql = """
        SELECT * FROM incidentes
        WHERE estado = ? 
        ORDER BY hora_incidente DESC
        """

        return try {
            // Consulta filtrada por estado con ordenamiento temporal
            jdbcTemplate.query(sql, incidenteRowMapper, estado)
        } catch (e: Exception) {
            // Logging con contexto del estado buscado
            logger.error("Error al buscar incidentes por estado $estado: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Busca incidentes cercanos a una ubicación geográfica específica
     * Utiliza fórmula de Haversine para calcular distancias en la superficie terrestre
     * Consulta geoespacial optimizada con cálculo de distancia en kilómetros
     * @param latitud Latitud del punto de referencia
     * @param longitud Longitud del punto de referencia
     * @param radioKm Radio de búsqueda en kilómetros
     * @return Lista de incidentes dentro del radio especificado, ordenados por distancia
     */
    fun findNearby(latitud: Double, longitud: Double, radioKm: Double): List<Incidente> {
        // Query con fórmula de Haversine para cálculo de distancia geográfica
        val sql = """
        SELECT *, 
        (6371 * acos(
            cos(radians(?)) * cos(radians(latitud)) *
            cos(radians(longitud) - radians(?)) +
            sin(radians(?)) * sin(radians(latitud))
        )) as distancia
        FROM incidentes
        HAVING distancia <= ?
        ORDER BY distancia, hora_incidente DESC
        """

        return try {
            // Ejecución de consulta geoespacial con múltiples parámetros de coordenadas
            jdbcTemplate.query(
                sql,
                incidenteRowMapper,
                latitud, longitud, latitud, radioKm
            )
        } catch (e: Exception) {
            // Logging de errores en consultas geoespaciales complejas
            logger.error("Error al buscar incidentes cercanos: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Actualiza únicamente el estado de un incidente específico
     * Operación optimizada que modifica solo el campo de estado
     * @param id Identificador único del incidente
     * @param estado Nuevo estado a establecer
     * @return Incidente actualizado o null si no se encontró o hubo errores
     */
    fun updateEstado(id: String, estado: String): Incidente? {
        val sql = "UPDATE incidentes SET estado = ? WHERE id = ?"

        return try {
            val jdbcTemplate = JdbcTemplate(dataSource)

            // Ejecución de UPDATE y verificación de filas afectadas
            val rowsAffected = jdbcTemplate.update(sql, estado, id)
            if (rowsAffected > 0) {
                // Recuperación del incidente actualizado para retornar datos frescos
                findById(id)
            } else {
                // Logging cuando no se encuentra el incidente a actualizar
                logger.warn("No se encontró el incidente con ID $id para actualizar estado")
                null
            }
        } catch (e: Exception) {
            // Manejo de errores durante actualización de estado
            logger.error("Error al actualizar estado del incidente $id: ${e.message}", e)
            null
        }
    }

    /**
     * Elimina físicamente un incidente de la base de datos
     * Operación irreversible que remueve completamente el registro
     * @param id Identificador único del incidente a eliminar
     * @return true si se eliminó exitosamente, false en caso contrario
     */
    fun eliminarIncidente(id: String): Boolean {
        val sql = "DELETE FROM incidentes WHERE id = ?"

        return try {
            val jdbcTemplate = JdbcTemplate(dataSource)

            // Ejecución de DELETE y verificación de filas afectadas
            val rowsAffected = jdbcTemplate.update(sql, id)
            rowsAffected > 0
        } catch (e: Exception) {
            // Logging de errores durante eliminación
            logger.error("Error al eliminar incidente con ID $id: ${e.message}", e)
            false
        }
    }

    /**
     * Verifica la existencia de un incidente por su identificador
     * Consulta optimizada de conteo para verificación de existencia
     * @param id Identificador único del incidente
     * @return true si el incidente existe, false en caso contrario
     */
    fun existsById(id: String): Boolean {
        val sql = "SELECT COUNT(*) FROM incidentes WHERE id = ?"

        return try {
            // Query de conteo optimizada para verificación de existencia
            val count = jdbcTemplate.queryForObject(sql, Int::class.java, id) ?: 0
            count > 0
        } catch (e: Exception) {
            // Retorno de false en caso de error de consulta
            logger.error("Error al verificar existencia del incidente $id: ${e.message}", e)
            false
        }
    }
}