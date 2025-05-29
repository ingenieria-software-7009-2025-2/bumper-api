package com.bumper.api.user.service

import com.bumper.api.user.domain.Incidente
import com.bumper.api.user.repository.IncidenteRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.annotation.Propagation

/**
 * Servicio de dominio para la gestión de incidentes viales.
 * Orquesta la lógica de negocio y delega la persistencia al repositorio.
 * Incluye validaciones de estado, permisos y lógica de enriquecimiento de datos.
 */
@Service
class IncidenteService(
    private val incidenteRepository: IncidenteRepository,
    private val usuarioService: UsuarioService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Crea un nuevo incidente en el sistema.
     * Valida el estado inicial y delega la persistencia al repositorio.
     * @param incidente Entidad Incidente a crear
     * @return Incidente creado y persistido
     * @throws IllegalArgumentException si el estado es inválido
     * @throws IllegalStateException si ocurre un error de persistencia
     */
    @Transactional
    fun crear(incidente: Incidente): Incidente {
        logger.info("Creando nuevo incidente para usuario ID: ${incidente.usuarioId}")

        // Validar el estado inicial del incidente
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
     * Obtiene todos los incidentes del sistema, enriquecidos con información básica del usuario.
     * @return Lista de mapas con datos de incidentes y usuario asociado
     */
    fun obtenerTodos(): List<Map<String, Any?>> {
        logger.info("Obteniendo todos los incidentes")
        return try {
            val incidentes = incidenteRepository.findAll()
            val usuarioIds = incidentes.map { it.usuarioId }.distinct()
            val usuarios = usuarioService.buscarPorIds(usuarioIds)
            val usuarioMap = usuarios.associateBy { it.id }

            // Enriquecer cada incidente con datos del usuario
            incidentes.map { incidente ->
                val usuario = usuarioMap[incidente.usuarioId]
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
                    "usuario" to usuario?.let {
                        mapOf(
                            "id" to it.id,
                            "nombre" to it.nombre,
                            "apellido" to it.apellido
                        )
                    }
                )
            }
        } catch (e: Exception) {
            logger.error("Error al obtener todos los incidentes: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Obtiene todos los incidentes asociados a un usuario específico.
     * @param usuarioId ID del usuario
     * @return Lista de incidentes del usuario
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
     * Obtiene un incidente por su identificador único.
     * @param id ID del incidente
     * @return Incidente encontrado o null si no existe
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
     * Actualiza el estado de un incidente.
     * Valida el nuevo estado antes de actualizar.
     * @param id ID del incidente
     * @param estado Nuevo estado a establecer
     * @return Incidente actualizado o null si no se encontró
     * @throws IllegalArgumentException si el estado es inválido
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
     * Obtiene incidentes filtrados por estado.
     * @param estado Estado a buscar (PENDIENTE, EN_PROCESO, RESUELTO)
     * @return Lista de incidentes con el estado especificado
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
     * Busca incidentes cercanos a una ubicación geográfica.
     * Valida las coordenadas y el radio antes de consultar.
     * @param latitud Latitud de referencia
     * @param longitud Longitud de referencia
     * @param radioKm Radio de búsqueda en kilómetros
     * @return Lista de incidentes cercanos
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
     * Valida si un usuario puede modificar un incidente.
     * Solo el usuario creador puede modificarlo.
     * @param incidenteId ID del incidente
     * @param usuarioId ID del usuario
     * @return true si el usuario puede modificar el incidente, false en caso contrario
     */
    fun puedeModificar(incidenteId: String, usuarioId: Long): Boolean {
        val incidente = obtenerPorId(incidenteId) ?: return false
        return incidente.usuarioId == usuarioId
    }

    /**
     * Elimina un incidente por su ID.
     * Verifica existencia antes de eliminar.
     * @param id ID del incidente a eliminar
     * @return true si se eliminó correctamente, false en caso contrario
     */
    @Transactional(propagation = Propagation.REQUIRED)
    fun eliminar(id: String): Boolean {
        logger.info("Eliminando incidente con ID: $id")
        return try {
            // Verificar si el incidente existe antes de intentar eliminarlo
            val incidente = obtenerPorId(id)
            if (incidente == null) {
                logger.warn("No se encontró el incidente con ID $id para eliminar")
                return false
            }
            incidenteRepository.eliminarIncidente(id)
        } catch (e: Exception) {
            logger.error("Error al eliminar incidente $id: ${e.message}", e)
            false
        }
    }

    /**
     * Valida coordenadas y radio para búsqueda de incidentes cercanos.
     * @param latitud Latitud a validar
     * @param longitud Longitud a validar
     * @param radioKm Radio a validar
     * @return true si los parámetros son válidos, false en caso contrario
     */
    private fun coordenadasValidas(latitud: Double, longitud: Double, radioKm: Double): Boolean {
        return latitud in -90.0..90.0 &&
                longitud in -180.0..180.0 &&
                radioKm > 0
    }

    companion object {
        // Estados válidos permitidos para los incidentes
        private val estadosValidos = setOf("PENDIENTE", "EN_PROCESO", "RESUELTO")
    }
}