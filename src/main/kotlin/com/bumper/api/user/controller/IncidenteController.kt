package com.bumper.api.user.controller

import com.bumper.api.user.domain.Incidente
import com.bumper.api.user.service.IncidenteService
import com.bumper.api.user.service.UsuarioService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/v1/incidentes")
class IncidenteController(
    private val incidenteService: IncidenteService,
    private val usuarioService: UsuarioService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Endpoint para crear un nuevo incidente en el sistema.
     */
    @PostMapping("/create")
    fun registrarIncidente(@RequestBody request: IncidenteRequest): ResponseEntity<Any> {
        logger.info("Recibida solicitud para registrar incidente: $request")
        return try {
            val usuario = usuarioService.buscarPorId(request.usuarioId)
                ?: return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(mapOf("mensaje" to "Usuario no encontrado"))

            val incidente = Incidente(
                usuario = usuario,
                tipoIncidente = request.tipoIncidente,
                ubicacion = request.ubicacion,
                latitud = request.latitud,
                longitud = request.longitud,
                horaIncidente = LocalDateTime.now(),
                tipoVialidad = request.tipoVialidad,
                estado = "PENDIENTE"
            )

            val incidenteCreado = incidenteService.crear(incidente)
            ResponseEntity.status(HttpStatus.CREATED).body(incidenteCreado)
        } catch (e: Exception) {
            logger.error("Error al registrar incidente: ${e.message}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("mensaje" to "Error al registrar el incidente: ${e.message}"))
        }
    }

    /**
     * Endpoint para obtener todos los incidentes registrados en el sistema.
     */
    @GetMapping("/all")
    fun obtenerTodos(): ResponseEntity<List<Incidente>> {
        logger.info("Obteniendo todos los incidentes")
        val incidentes = incidenteService.obtenerTodos()
        return ResponseEntity.ok(incidentes)
    }

    /**
     * Endpoint para obtener todos los incidentes asociados a un usuario específico.
     */
    @GetMapping("/usuario/{usuarioId}")
    fun obtenerPorUsuario(@PathVariable usuarioId: Long): ResponseEntity<List<Incidente>> {
        logger.info("Obteniendo incidentes para el usuario con ID: $usuarioId")
        val incidentes = incidenteService.obtenerPorUsuario(usuarioId)
        if (incidentes.isEmpty()) {
            logger.warn("No se encontraron incidentes para el usuario con ID: $usuarioId")
        }
        return ResponseEntity.ok(incidentes)
    }

    /**
     * Endpoint para obtener un incidente por su ID.
     */
    @GetMapping("/{id}")
    fun obtenerPorId(@PathVariable id: String): ResponseEntity<Any> {
        logger.info("Buscando incidente con ID: $id")
        val incidente = incidenteService.obtenerPorId(id)
        return if (incidente != null) {
            ResponseEntity.ok(incidente)
        } else {
            logger.warn("No se encontró incidente con ID: $id")
            ResponseEntity.status(HttpStatus.NOT_FOUND).body("Incidente no encontrado")
        }
    }

    /**
     * Endpoint para actualizar el estado de un incidente.
     */
    @PutMapping("/update-status/{id}")
    fun actualizarEstado(
        @PathVariable id: String,
        @RequestBody request: EstadoRequest,
        @RequestParam usuarioId: Long
    ): ResponseEntity<Any> {
        logger.info("Actualizando estado del incidente con ID: $id a ${request.estado}")

        if (!incidenteService.puedeModificar(id, usuarioId)) {
            return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body("No tienes permiso para modificar este incidente")
        }

        return try {
            val incidenteActualizado = incidenteService.actualizarEstado(id, request.estado)
            if (incidenteActualizado != null) {
                ResponseEntity.ok(incidenteActualizado)
            } else {
                logger.warn("No se encontró incidente con ID: $id para actualizar estado")
                ResponseEntity.status(HttpStatus.NOT_FOUND).body("Incidente no encontrado")
            }
        } catch (e: Exception) {
            logger.error("Error al actualizar estado del incidente: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al actualizar el estado del incidente")
        }
    }

    /**
     * Endpoint para buscar incidentes cercanos a una ubicación geográfica.
     */
    @GetMapping("/cercanos")
    fun buscarCercanos(
        @RequestParam latitud: Double,
        @RequestParam longitud: Double,
        @RequestParam(defaultValue = "5.0") radioKm: Double
    ): ResponseEntity<List<Incidente>> {
        logger.info("Buscando incidentes cercanos a latitud: $latitud, longitud: $longitud, radio: $radioKm km")
        val incidentes = incidenteService.buscarCercanos(latitud, longitud, radioKm)
        return ResponseEntity.ok(incidentes)
    }
}

/**
 * DTO para la solicitud de creación de incidente.
 */
data class IncidenteRequest(
    val usuarioId: Long,
    val tipoIncidente: String,
    val ubicacion: String,
    val latitud: Double,
    val longitud: Double,
    val tipoVialidad: String
)

/**
 * DTO para actualizar el estado de un incidente.
 */
data class EstadoRequest(
    val estado: String
)