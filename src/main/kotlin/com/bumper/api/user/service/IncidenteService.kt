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
        logger.info("Creando nuevo incidente")
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
    fun obtenerPorId(id: String): Incidente? {
        logger.info("Buscando incidente con ID: $id")
        return incidenteRepository.findById(id)
    }

    /**
     * Actualiza el estado de un incidente
     */
    @Transactional
    fun actualizarEstado(id: String, nuevoEstado: String): Incidente? {
        logger.info("Actualizando estado de incidente $id a: $nuevoEstado")
        return incidenteRepository.updateEstado(id, nuevoEstado)
    }

    /**
     * Obtiene incidentes por estado
     */
    fun obtenerPorEstado(estado: String): List<Incidente> {
        logger.info("Obteniendo incidentes con estado: $estado")
        return incidenteRepository.findByEstado(estado)
    }

    /**
     * Busca incidentes cercanos a una ubicación
     */
    fun buscarCercanos(latitud: Double, longitud: Double, radioKm: Double): List<Incidente> {
        logger.info("Buscando incidentes cercanos a ($latitud, $longitud) en radio de $radioKm km")
        return incidenteRepository.findNearby(latitud, longitud, radioKm)
    }

    /**
     * Valida si un usuario puede modificar un incidente
     */
    fun puedeModificar(incidenteId: String, usuarioId: Long): Boolean {
        val incidente = obtenerPorId(incidenteId) ?: return false
        return incidente.usuario.id == usuarioId
    }
}