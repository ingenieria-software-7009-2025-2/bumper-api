package com.bumper.api.user.service

import com.bumper.api.user.domain.FotoIncidente
import com.bumper.api.user.repository.FotoIncidenteRepository
import org.springframework.stereotype.Service

@Service
class FotoIncidenteService(private val fotoIncidenteRepository: FotoIncidenteRepository) {

    /**
     * Guarda una nueva foto asociada a un incidente.
     */
    fun guardarFoto(foto: FotoIncidente): Long {
        return fotoIncidenteRepository.save(foto)
    }

    /**
     * Obtiene todas las fotos asociadas a un incidente.
     */
    fun obtenerFotosPorIncidente(incidenteId: String): List<FotoIncidente> {
        return fotoIncidenteRepository.findByIncidenteId(incidenteId)
    }

    /**
     * Elimina una foto por su ID.
     */
    fun eliminarFoto(id: Long) {
        fotoIncidenteRepository.delete(id)
    }

    /**
     * Elimina todas las fotos de un incidente.
     */
    fun eliminarFotosPorIncidente(incidenteId: String) {
        fotoIncidenteRepository.deleteByIncidenteId(incidenteId)
    }

    /**
     * Actualiza la descripción de una foto.
     */
    fun actualizarDescripcion(id: Long, descripcion: String) {
        fotoIncidenteRepository.updateDescripcion(id, descripcion)
    }

    /**
     * Verifica si una foto pertenece a un usuario específico.
     */
    fun perteneceAUsuario(fotoId: Long, usuarioId: Long): Boolean {
        return fotoIncidenteRepository.belongsToUser(fotoId, usuarioId)
    }

    /**
     * Cuenta cuántas fotos tiene un incidente.
     */
    fun contarFotosPorIncidente(incidenteId: String): Int {
        return fotoIncidenteRepository.countFotosByIncidenteId(incidenteId)
    }
}