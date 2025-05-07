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

@Repository
class IncidenteRepository(
    private val dataSource: DataSource,
    private val usuarioRepository: UsuarioRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val jdbcTemplate = JdbcTemplate(dataSource)

    private val incidenteRowMapper = RowMapper { rs, _ ->
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

    @Transactional
    fun save(incidente: Incidente): Incidente {
        try {
            if (!usuarioRepository.existsById(incidente.usuarioId)) {
                throw IllegalStateException("El usuario con ID ${incidente.usuarioId} no existe")
            }

            val idGenerado = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) +
                    "_${incidente.tipoIncidente.take(3)}_${incidente.tipoVialidad.take(3)}"

            logger.info("Guardando incidente con ID generado: $idGenerado")

            val sql = """
                INSERT INTO incidentes (
                    id, usuario_id, tipo_incidente, ubicacion, 
                    latitud, longitud, tipo_vialidad, 
                    estado, hora_incidente, fotos
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """

            // Crear array SQL para las fotos de manera explícita
            val fotosArray = jdbcTemplate.execute(ConnectionCallback { connection ->
                connection.createArrayOf("text", incidente.fotos.toTypedArray())
            })

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

            return findById(idGenerado)
                ?: throw IllegalStateException("No se pudo recuperar el incidente guardado")

        } catch (e: Exception) {
            logger.error("Error al guardar incidente: ${e.message}", e)
            throw IllegalStateException("Error al guardar el incidente: ${e.message}")
        }
    }

    fun findAll(): List<Incidente> {
        val sql = """
            SELECT * FROM incidentes
            ORDER BY hora_incidente DESC
        """

        return try {
            jdbcTemplate.query(sql, incidenteRowMapper)
        } catch (e: Exception) {
            logger.error("Error al obtener todos los incidentes: ${e.message}", e)
            emptyList()
        }
    }

    fun findByUsuarioId(usuarioId: Long): List<Incidente> {
        val sql = """
            SELECT * FROM incidentes
            WHERE usuario_id = ?
            ORDER BY hora_incidente DESC
        """

        return try {
            jdbcTemplate.query(sql, incidenteRowMapper, usuarioId)
        } catch (e: Exception) {
            logger.error("Error al buscar incidentes por usuario ID $usuarioId: ${e.message}", e)
            emptyList()
        }
    }

    @Transactional(readOnly = true)
    fun findById(id: String): Incidente? {
        val sql = "SELECT * FROM incidentes WHERE id = ?"
        return try {
            jdbcTemplate.query(sql, incidenteRowMapper, id).firstOrNull()
        } catch (e: Exception) {
            logger.error("Error al buscar incidente por ID $id: ${e.message}", e)
            null
        }
    }

    fun findByEstado(estado: String): List<Incidente> {
        val sql = """
            SELECT * FROM incidentes
            WHERE estado = ? 
            ORDER BY hora_incidente DESC
        """
        return try {
            jdbcTemplate.query(sql, incidenteRowMapper, estado)
        } catch (e: Exception) {
            logger.error("Error al buscar incidentes por estado $estado: ${e.message}", e)
            emptyList()
        }
    }

    fun findNearby(latitud: Double, longitud: Double, radioKm: Double): List<Incidente> {
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
            jdbcTemplate.query(
                sql,
                incidenteRowMapper,
                latitud, longitud, latitud, radioKm
            )
        } catch (e: Exception) {
            logger.error("Error al buscar incidentes cercanos: ${e.message}", e)
            emptyList()
        }
    }

    @Transactional
    fun updateEstado(id: String, estado: String): Incidente? {
        val sql = "UPDATE incidentes SET estado = ? WHERE id = ?"
        return try {
            val rowsAffected = jdbcTemplate.update(sql, estado, id)
            if (rowsAffected > 0) {
                findById(id)
            } else {
                logger.warn("No se encontró el incidente con ID $id para actualizar estado")
                null
            }
        } catch (e: Exception) {
            logger.error("Error al actualizar estado del incidente $id: ${e.message}", e)
            null
        }
    }

    fun existsById(id: String): Boolean {
        val sql = "SELECT COUNT(*) FROM incidentes WHERE id = ?"
        return try {
            val count = jdbcTemplate.queryForObject(sql, Int::class.java, id) ?: 0
            count > 0
        } catch (e: Exception) {
            logger.error("Error al verificar existencia del incidente $id: ${e.message}", e)
            false
        }
    }
}