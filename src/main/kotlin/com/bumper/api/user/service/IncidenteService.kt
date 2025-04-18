package com.bumper.api.user.service

import com.bumper.api.user.domain.Incidente
import com.bumper.api.user.repository.IncidenteRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class IncidenteService(
    private val incidenteRepository: IncidenteRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Crea un nuevo incidente
     */
    @Transactional
    fun crear(incidente: Incidente): Incidente {
        logger.info("Creando nuevo incidente para usuario ID: ${incidente.usuario.id}")
        return incidenteRepository.save(incidente)
    }

    /**
     * Obtiene todos los incidentes
     */
    fun obtenerTodos(): List<Incidente> {
        logger.info("Obteniendo todos los incidentes")
        return incidenteRepository.findAll()
    }

    /**
     * Obtiene los incidentes de un usuario específico
     */
    fun obtenerPorUsuario(usuarioId: Long): List<Incidente> {
        logger.info("Obteniendo incidentes para usuario ID: $usuarioId")
        return incidenteRepository.findByUsuarioId(usuarioId)
    }

    /**
     * Obtiene un incidente por su ID
     */
    @Transactional(readOnly = true)
    fun obtenerPorId(id: String): Incidente? {
        logger.info("Buscando incidente con ID: $id")
        return incidenteRepository.findById(id)
    }

    /**
     * Actualiza el estado de un incidente
     */
    @Transactional
    fun actualizarEstado(id: String, estado: String): Incidente? {
        logger.info("Actualizando estado de incidente $id a: $estado")
        // Validar que el estado sea válido
        if (!estadosValidos.contains(estado.uppercase())) {
            logger.error("Estado inválido: $estado")
            throw IllegalArgumentException("Estado inválido. Estados válidos: ${estadosValidos.joinToString()}")
        }
        return incidenteRepository.updateEstado(id, estado.uppercase())
    }

    /**
     * Obtiene incidentes por estado
     */
    fun obtenerPorEstado(estado: String): List<Incidente> {
        logger.info("Obteniendo incidentes con estado: $estado")
        // Validar que el estado sea válido
        if (!estadosValidos.contains(estado.uppercase())) {
            logger.error("Estado inválido: $estado")
            return emptyList()
        }
        return incidenteRepository.findByEstado(estado.uppercase())
    }

    /**
     * Busca incidentes cercanos a una ubicación
     */
    fun buscarCercanos(latitud: Double, longitud: Double, radioKm: Double): List<Incidente> {
        logger.info("Buscando incidentes cercanos a ($latitud, $longitud) en radio de $radioKm km")
        // Validar coordenadas
        if (latitud < -90 || latitud > 90 || longitud < -180 || longitud > 180 || radioKm <= 0) {
            logger.error("Coordenadas o radio inválidos: lat=$latitud, lon=$longitud, radio=$radioKm")
            return emptyList()
        }
        return incidenteRepository.findNearby(latitud, longitud, radioKm)
    }

    /**
     * Valida si un usuario puede modificar un incidente
     */
    fun puedeModificar(incidenteId: String, usuarioId: Long): Boolean {
        val incidente = obtenerPorId(incidenteId) ?: return false
        return incidente.usuario.id == usuarioId
    }

    companion object {
        private val estadosValidos = setOf("PENDIENTE", "EN_PROCESO", "RESUELTO")
    }
}