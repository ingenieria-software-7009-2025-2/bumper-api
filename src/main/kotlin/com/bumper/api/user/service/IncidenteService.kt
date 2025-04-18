package com.bumper.api.user.service

import com.bumper.api.user.domain.Incidente
import com.bumper.api.user.repository.IncidenteRepository
import org.springframework.stereotype.Service

@Service
class IncidenteService(private val incidenteRepository: IncidenteRepository) {

    /**
     * Registra un nuevo incidente.
     */
    fun registrarIncidente(incidente: Incidente): Incidente {
        return incidenteRepository.save(incidente)
    }

    /**
     * Obtiene un incidente por su ID.
     */
    fun obtenerPorId(id: String): Incidente? {
        return incidenteRepository.findById(id)
    }

    /**
     * Obtiene todos los incidentes.
     */
    fun obtenerTodos(): List<Incidente> {
        return incidenteRepository.findAll()
    }

    /**
     * Obtiene los incidentes reportados por un usuario específico.
     */
    fun obtenerPorUsuario(usuarioId: Long): List<Incidente> {
        return incidenteRepository.findByUsuarioId(usuarioId)
    }

    /**
     * Actualiza el estado de un incidente.
     */
    fun actualizarEstado(id: String, estado: String): Incidente? {
        return incidenteRepository.updateEstado(id, estado)
    }

    /**
     * Obtiene incidentes por estado.
     */
    fun obtenerPorEstado(estado: String): List<Incidente> {
        return incidenteRepository.findByEstado(estado)
    }

    /**
     * Busca incidentes cercanos a una ubicación geográfica.
     */
    fun buscarCercanos(latitud: Double, longitud: Double, radioKm: Double): List<Incidente> {
        return incidenteRepository.findNearby(latitud, longitud, radioKm)
    }
}