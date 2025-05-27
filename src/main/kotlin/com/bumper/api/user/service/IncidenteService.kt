package com.bumper.api.user.service

import com.bumper.api.user.domain.Incidente
import com.bumper.api.user.repository.IncidenteRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class IncidenteService(
    private val incidenteRepository: IncidenteRepository,
    private val usuarioService: UsuarioService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Crea un nuevo incidente
     */
    @Transactional
    fun crear(incidente: Incidente): Incidente {
        logger.info("Creando nuevo incidente para usuario ID: ${incidente.usuarioId}")

        // Validar el estado inicial
        val estadoInicial = incidente.estado.uppercase()
        if (!estadosValidos.contains(estadoInicial)) {
            logger.error("Estado inicial inválido: $estadoInicial")
            throw IllegalArgumentException("Estado inválido. Estados válidos: ${estadosValidos.joinToString()}")
        }

        return try {
            incidenteRepository.save(incidente)
        } catch (e: Exception) {
            logger.error("Error al crear incidente: ${e.message}", e)
            throw IllegalStateException("No se pudo crear el incidente: ${e.message}")
        }
    }

    /**
     * Obtiene todos los incidentes
     */

    fun obtenerTodos(): List<Map<String, Any?>> {
        logger.info("Obteniendo todos los incidentes")
        return try {
            val incidentes = incidenteRepository.findAll()

            // Convertir cada incidente a un mapa y añadir la información del usuario
            incidentes.map { incidente ->
                val usuario = usuarioService.buscarPorId(incidente.usuarioId)
                mapOf(
                    "id" to incidente.id,
                    "usuarioId" to incidente.usuarioId,
                    "tipoIncidente" to incidente.tipoIncidente,
                    "ubicacion" to incidente.ubicacion,
                    "latitud" to incidente.latitud,
                    "longitud" to incidente.longitud,
                    "horaIncidente" to incidente.horaIncidente,
                    "tipoVialidad" to incidente.tipoVialidad,
                    "estado" to incidente.estado,
                    "fotos" to incidente.fotos,
                    "usuario" to if (usuario != null) {
                        mapOf(
                            "id" to usuario.id,
                            "nombre" to usuario.nombre,
                            "apellido" to usuario.apellido
                        )
                    } else null
                )
            }
        } catch (e: Exception) {
            logger.error("Error al obtener todos los incidentes: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Obtiene los incidentes de un usuario específico
     */
    fun obtenerPorUsuario(usuarioId: Long): List<Incidente> {
        logger.info("Obteniendo incidentes para usuario ID: $usuarioId")
        return try {
            incidenteRepository.findByUsuarioId(usuarioId)
        } catch (e: Exception) {
            logger.error("Error al obtener incidentes del usuario $usuarioId: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Obtiene un incidente por su ID
     */
    @Transactional(readOnly = true)
    fun obtenerPorId(id: String): Incidente? {
        logger.info("Buscando incidente con ID: $id")
        return try {
            incidenteRepository.findById(id)
        } catch (e: Exception) {
            logger.error("Error al buscar incidente $id: ${e.message}", e)
            null
        }
    }

    /**
     * Actualiza el estado de un incidente
     */
    @Transactional
    fun actualizarEstado(id: String, estado: String): Incidente? {
        logger.info("Actualizando estado de incidente $id a: $estado")

        // Validar que el estado sea válido
        val nuevoEstado = estado.uppercase()
        if (!estadosValidos.contains(nuevoEstado)) {
            logger.error("Estado inválido: $estado")
            throw IllegalArgumentException("Estado inválido. Estados válidos: ${estadosValidos.joinToString()}")
        }

        return try {
            incidenteRepository.updateEstado(id, nuevoEstado)
        } catch (e: Exception) {
            logger.error("Error al actualizar estado del incidente $id: ${e.message}", e)
            null
        }
    }

    /**
     * Obtiene incidentes por estado
     */
    fun obtenerPorEstado(estado: String): List<Incidente> {
        logger.info("Obteniendo incidentes con estado: $estado")

        // Validar que el estado sea válido
        val estadoBusqueda = estado.uppercase()
        if (!estadosValidos.contains(estadoBusqueda)) {
            logger.error("Estado inválido: $estado")
            return emptyList()
        }

        return try {
            incidenteRepository.findByEstado(estadoBusqueda)
        } catch (e: Exception) {
            logger.error("Error al obtener incidentes por estado $estado: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Busca incidentes cercanos a una ubicación
     */
    fun buscarCercanos(latitud: Double, longitud: Double, radioKm: Double): List<Incidente> {
        logger.info("Buscando incidentes cercanos a ($latitud, $longitud) en radio de $radioKm km")

        // Validar coordenadas y radio
        if (!coordenadasValidas(latitud, longitud, radioKm)) {
            logger.error("Coordenadas o radio inválidos: lat=$latitud, lon=$longitud, radio=$radioKm")
            return emptyList()
        }

        return try {
            incidenteRepository.findNearby(latitud, longitud, radioKm)
        } catch (e: Exception) {
            logger.error("Error al buscar incidentes cercanos: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Valida si un usuario puede modificar un incidente
     */
    fun puedeModificar(incidenteId: String, usuarioId: Long): Boolean {
        val incidente = obtenerPorId(incidenteId) ?: return false
        return incidente.usuarioId == usuarioId
    }

    /**
     * Valida coordenadas y radio para búsqueda de incidentes cercanos
     */
    private fun coordenadasValidas(latitud: Double, longitud: Double, radioKm: Double): Boolean {
        return latitud in -90.0..90.0 &&
                longitud in -180.0..180.0 &&
                radioKm > 0
    }

    companion object {
        private val estadosValidos = setOf("PENDIENTE", "EN_PROCESO", "RESUELTO")

        // Constantes para validación
        const val RADIO_MAXIMO_KM = 50.0
    }
}