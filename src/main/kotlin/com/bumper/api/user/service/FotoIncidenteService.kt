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

    @Transactional
    fun agregarFoto(incidenteId: String, urlFoto: String, descripcion: String?): FotoIncidente {
        logger.info("Intentando agregar foto para incidente: $incidenteId")

        // Verificar que el incidente existe
        incidenteRepository.findById(incidenteId)
            ?: throw IllegalArgumentException("Incidente no encontrado: $incidenteId")

        return try {
            val foto = FotoIncidente(
                incidenteId = incidenteId,
                urlFoto = urlFoto,
                descripcion = descripcion
            )
            fotoIncidenteRepository.save(foto)
        } catch (e: Exception) {
            logger.error("Error al guardar la foto: ${e.message}", e)
            throw IllegalStateException("Error al guardar la foto: ${e.message}")
        }
    }

    /**
     * Obtiene todas las fotos de un incidente.
     * @throws IllegalArgumentException si el incidente no existe
     */
    fun getFotosDeIncidente(incidenteId: String): List<FotoIncidente> {
        logger.info("Obteniendo fotos del incidente: $incidenteId")

        // Verificar que el incidente existe
        incidenteRepository.findById(incidenteId)
            ?: throw IllegalArgumentException("Incidente no encontrado: $incidenteId")

        return fotoIncidenteRepository.findByIncidenteId(incidenteId)
    }

    /**
     * Elimina una foto específica, verificando permisos.
     * @throws IllegalArgumentException si la foto no existe o no pertenece al usuario
     */
    @Transactional
    fun eliminarFoto(id: Long, usuarioId: Long) {
        logger.info("Intentando eliminar foto ID: $id")

        if (!perteneceAUsuario(id, usuarioId)) {
            throw IllegalArgumentException("No tienes permiso para eliminar esta foto")
        }

        try {
            fotoIncidenteRepository.delete(id)
            logger.info("Foto eliminada exitosamente")
        } catch (e: Exception) {
            logger.error("Error al eliminar la foto: ${e.message}", e)
            throw IllegalStateException("Error al eliminar la foto: ${e.message}")
        }
    }

    /**
     * Elimina todas las fotos de un incidente, verificando permisos.
     * @throws IllegalArgumentException si el incidente no existe o no pertenece al usuario
     */
    @Transactional
    fun eliminarFotosPorIncidente(incidenteId: String, usuarioId: Long) {
        logger.info("Intentando eliminar todas las fotos del incidente: $incidenteId")
        val incidente = incidenteRepository.findById(incidenteId)
            ?: throw IllegalArgumentException("Incidente no encontrado: $incidenteId")
        if (incidente.usuario.id != usuarioId) {
            throw IllegalArgumentException("No tienes permiso para eliminar las fotos de este incidente")
        }
        val fotosEliminadas = fotoIncidenteRepository.deleteByIncidenteId(incidenteId)
        logger.info("$fotosEliminadas fotos eliminadas del incidente $incidenteId")
    }

    /**
     * Actualiza la descripción de una foto, verificando permisos.
     * @throws IllegalArgumentException si la foto no existe o no pertenece al usuario
     */
    @Transactional
    fun actualizarDescripcion(id: Long, descripcion: String, usuarioId: Long) {
        logger.info("Intentando actualizar descripción de foto ID: $id")

        if (!perteneceAUsuario(id, usuarioId)) {
            throw IllegalArgumentException("No tienes permiso para modificar esta foto")
        }

        try {
            fotoIncidenteRepository.updateDescripcion(id, descripcion)
            logger.info("Descripción actualizada exitosamente")
        } catch (e: Exception) {
            logger.error("Error al actualizar la descripción: ${e.message}", e)
            throw IllegalStateException("Error al actualizar la descripción: ${e.message}")
        }
    }

    /**
     * Verifica si una foto pertenece a un usuario específico.
     */
    fun perteneceAUsuario(fotoId: Long, usuarioId: Long): Boolean {
        return fotoIncidenteRepository.belongsToUser(fotoId, usuarioId)
    }

    /**
     * Cuenta el número de fotos que tiene un incidente.
     */
    fun contarFotosPorIncidente(incidenteId: String): Int {
        return fotoIncidenteRepository.countFotosByIncidenteId(incidenteId)
    }
}