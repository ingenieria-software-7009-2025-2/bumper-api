package com.bumper.api.user.service

import com.bumper.api.user.domain.FotoIncidente
import com.bumper.api.user.repository.FotoIncidenteRepository
import com.bumper.api.user.repository.IncidenteRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FotoIncidenteService(
    private val fotoIncidenteRepository: FotoIncidenteRepository,
    private val incidenteRepository: IncidenteRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Agrega una nueva foto a un incidente
     */
    @Transactional
    fun agregarFoto(incidenteId: String, urlFoto: String, descripcion: String? = null): FotoIncidente {
        logger.info("Agregando foto para incidente: $incidenteId")

        // Verificar si existe el incidente
        if (!incidenteRepository.existsById(incidenteId)) {
            throw IllegalArgumentException("El incidente no existe")
        }

        // Verificar límite de fotos
        if (fotoIncidenteRepository.countFotosByIncidenteId(incidenteId) >= MAX_FOTOS_POR_INCIDENTE) {
            throw IllegalStateException("Se ha alcanzado el límite máximo de fotos para este incidente")
        }

        return try {
            val foto = FotoIncidente(
                incidenteId = incidenteId,
                urlFoto = urlFoto,
                descripcion = descripcion
            )
            fotoIncidenteRepository.save(foto)
        } catch (e: Exception) {
            logger.error("Error al agregar foto: ${e.message}", e)
            throw IllegalStateException("Error al guardar la foto: ${e.message}")
        }
    }

    /**
     * Obtiene todas las fotos de un incidente
     */
    fun getFotosDeIncidente(incidenteId: String): List<FotoIncidente> {
        logger.info("Obteniendo fotos del incidente: $incidenteId")
        return fotoIncidenteRepository.findByIncidenteId(incidenteId)
    }

    /**
     * Elimina una foto específica
     */
    @Transactional
    fun eliminarFoto(fotoId: Long, usuarioId: Long) {
        logger.info("Eliminando foto $fotoId (solicitado por usuario $usuarioId)")

        // Verificar si la foto existe y pertenece al usuario
        if (!fotoIncidenteRepository.belongsToUser(fotoId, usuarioId)) {
            throw IllegalArgumentException("No tienes permiso para eliminar esta foto")
        }

        try {
            if (!fotoIncidenteRepository.deleteById(fotoId)) {
                throw IllegalStateException("No se pudo eliminar la foto")
            }
        } catch (e: Exception) {
            logger.error("Error al eliminar foto: ${e.message}", e)
            throw IllegalStateException("Error al eliminar la foto: ${e.message}")
        }
    }

    /**
     * Actualiza la descripción de una foto
     */
    @Transactional
    fun actualizarDescripcion(fotoId: Long, descripcion: String, usuarioId: Long) {
        logger.info("Actualizando descripción de foto $fotoId")

        // Verificar si la foto existe y pertenece al usuario
        if (!fotoIncidenteRepository.belongsToUser(fotoId, usuarioId)) {
            throw IllegalArgumentException("No tienes permiso para modificar esta foto")
        }

        try {
            if (!fotoIncidenteRepository.updateDescripcion(fotoId, descripcion)) {
                throw IllegalStateException("No se pudo actualizar la descripción")
            }
        } catch (e: Exception) {
            logger.error("Error al actualizar descripción: ${e.message}", e)
            throw IllegalStateException("Error al actualizar la descripción: ${e.message}")
        }
    }

    companion object {
        const val MAX_FOTOS_POR_INCIDENTE = 5
    }
}