package com.bumper.api.user.repository

import com.bumper.api.user.domain.Incidente
import com.bumper.api.user.domain.FotoIncidente
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import javax.sql.DataSource
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Repository
class IncidenteRepository(
    private val dataSource: DataSource,
    private val usuarioRepository: UsuarioRepository,
    private val fotoIncidenteRepository: FotoIncidenteRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val jdbcTemplate = JdbcTemplate(dataSource)

    private val incidenteRowMapper = RowMapper { rs, _ ->
        Incidente(
            id = rs.getString("id"),
            usuarioId = rs.getLong("usuario_id"),
            tipoIncidente = rs.getString("tipo_incidente"),
            ubicacion = rs.getString("ubicacion"),
            latitud = rs.getDouble("latitud"),
            longitud = rs.getDouble("longitud"),
            horaIncidente = rs.getTimestamp("hora_incidente").toLocalDateTime(),
            tipoVialidad = rs.getString("tipo_vialidad"),
            estado = rs.getString("estado")
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
                    estado, hora_incidente
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """

            jdbcTemplate.update(sql,
                idGenerado,
                incidente.usuarioId,
                incidente.tipoIncidente,
                incidente.ubicacion,
                incidente.latitud,
                incidente.longitud,
                incidente.tipoVialidad,
                incidente.estado,
                Timestamp.valueOf(incidente.horaIncidente)
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
            val incidentes = jdbcTemplate.query(sql, incidenteRowMapper)
            incidentes.map { incidente ->
                incidente.id?.let { id ->
                    incidente.copyWithFotos(getFotosIncidente(id))
                } ?: incidente
            }
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
            val incidentes = jdbcTemplate.query(sql, incidenteRowMapper, usuarioId)
            incidentes.map { incidente ->
                incidente.id?.let { id ->
                    incidente.copyWithFotos(getFotosIncidente(id))
                } ?: incidente
            }
        } catch (e: Exception) {
            logger.error("Error al buscar incidentes por usuario ID $usuarioId: ${e.message}", e)
            emptyList()
        }
    }

    @Transactional(readOnly = true)
    fun findById(id: String): Incidente? {
        val sql = "SELECT * FROM incidentes WHERE id = ?"
        return try {
            jdbcTemplate.query(sql, incidenteRowMapper, id)
                .firstOrNull()
                ?.let { incidente ->
                    incidente.copyWithFotos(getFotosIncidente(id))
                }
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
            val incidentes = jdbcTemplate.query(sql, incidenteRowMapper, estado)
            incidentes.map { incidente ->
                incidente.id?.let { id ->
                    incidente.copyWithFotos(getFotosIncidente(id))
                } ?: incidente
            }
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
            val incidentes = jdbcTemplate.query(
                sql,
                incidenteRowMapper,
                latitud, longitud, latitud, radioKm
            )
            incidentes.map { incidente ->
                incidente.id?.let { id ->
                    incidente.copyWithFotos(getFotosIncidente(id))
                } ?: incidente
            }
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

    private fun getFotosIncidente(incidenteId: String): List<FotoIncidente> {
        return fotoIncidenteRepository.findByIncidenteId(incidenteId)
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