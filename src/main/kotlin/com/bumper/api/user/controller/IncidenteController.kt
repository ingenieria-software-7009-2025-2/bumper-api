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
                    .body(mapOf(
                        "mensaje" to "Usuario no encontrado",
                        "usuarioId" to request.usuarioId
                    ))

            val incidente = Incidente(
                usuarioId = request.usuarioId,
                tipoIncidente = request.tipoIncidente,
                ubicacion = request.ubicacion,
                latitud = request.latitud,
                longitud = request.longitud,
                tipoVialidad = request.tipoVialidad,
                estado = "PENDIENTE"
            )

            val incidenteCreado = incidenteService.crear(incidente)
            ResponseEntity.status(HttpStatus.CREATED).body(
                mapOf(
                    "mensaje" to "Incidente creado exitosamente",
                    "incidente" to incidenteCreado
                )
            )
        } catch (e: Exception) {
            logger.error("Error al registrar incidente: ${e.message}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf(
                    "mensaje" to "Error al registrar el incidente",
                    "error" to e.message
                ))
        }
    }

    /**
     * Endpoint para obtener todos los incidentes registrados en el sistema.
     */
    @GetMapping("/all")
    fun obtenerTodos(): ResponseEntity<Any> {
        logger.info("Obteniendo todos los incidentes")
        return try {
            val incidentes = incidenteService.obtenerTodos()
            if (incidentes.isEmpty()) {
                ResponseEntity.ok(
                    mapOf(
                        "mensaje" to "No se encontraron incidentes registrados",
                        "incidentes" to incidentes
                    )
                )
            } else {
                ResponseEntity.ok(
                    mapOf(
                        "mensaje" to "Incidentes encontrados",
                        "total" to incidentes.size,
                        "incidentes" to incidentes
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("Error al obtener todos los incidentes: ${e.message}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("mensaje" to "Error al obtener los incidentes"))
        }
    }

    /**
     * Endpoint para obtener todos los incidentes asociados a un usuario específico.
     */
    @GetMapping("/usuario/{usuarioId}")
    fun obtenerPorUsuario(@PathVariable usuarioId: Long): ResponseEntity<Any> {
        logger.info("Obteniendo incidentes para el usuario con ID: $usuarioId")
        return try {
            val usuario = usuarioService.buscarPorId(usuarioId)
                ?: return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(mapOf("mensaje" to "Usuario no encontrado"))

            val incidentes = incidenteService.obtenerPorUsuario(usuarioId)

            // Enriquecer cada incidente con la información del usuario
            val incidentesConUsuario = incidentes.map { incidente ->
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
                    "usuario" to mapOf(
                        "id" to usuario.id,
                        "nombre" to usuario.nombre,
                        "apellido" to usuario.apellido
                    )
                )
            }

            ResponseEntity.ok(
                mapOf(
                    "mensaje" to "Incidentes encontrados para el usuario",
                    "usuario" to mapOf(
                        "id" to usuario.id,
                        "nombre" to "${usuario.nombre} ${usuario.apellido}"
                    ),
                    "total" to incidentes.size,
                    "incidentes" to incidentesConUsuario
                )
            )
        } catch (e: Exception) {
            logger.error("Error al obtener incidentes del usuario $usuarioId: ${e.message}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("mensaje" to "Error al obtener los incidentes del usuario"))
        }
    }

    /**
     * Endpoint para obtener un incidente por su ID.
     */
    @GetMapping("/{id}")
    fun obtenerPorId(@PathVariable id: String): ResponseEntity<Any> {
        logger.info("Buscando incidente con ID: $id")
        return try {
            val incidente = incidenteService.obtenerPorId(id)
            if (incidente != null) {
                ResponseEntity.ok(
                    mapOf(
                        "mensaje" to "Incidente encontrado",
                        "incidente" to incidente
                    )
                )
            } else {
                ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(mapOf("mensaje" to "Incidente no encontrado"))
            }
        } catch (e: Exception) {
            logger.error("Error al buscar incidente con ID $id: ${e.message}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("mensaje" to "Error al buscar el incidente"))
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

        return try {
            if (!incidenteService.puedeModificar(id, usuarioId)) {
                return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(mapOf("mensaje" to "No tienes permiso para modificar este incidente"))
            }

            val incidenteActualizado = incidenteService.actualizarEstado(id, request.estado)
            if (incidenteActualizado != null) {
                ResponseEntity.ok(
                    mapOf(
                        "mensaje" to "Estado actualizado correctamente",
                        "incidente" to incidenteActualizado
                    )
                )
            } else {
                ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(mapOf("mensaje" to "Incidente no encontrado"))
            }
        } catch (e: Exception) {
            logger.error("Error al actualizar estado del incidente $id: ${e.message}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("mensaje" to "Error al actualizar el estado del incidente"))
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
    ): ResponseEntity<Any> {
        logger.info("Buscando incidentes cercanos a latitud: $latitud, longitud: $longitud, radio: $radioKm km")
        return try {
            val incidentes = incidenteService.buscarCercanos(latitud, longitud, radioKm)
            ResponseEntity.ok(
                mapOf(
                    "mensaje" to "Búsqueda completada",
                    "parametros" to mapOf(
                        "latitud" to latitud,
                        "longitud" to longitud,
                        "radioKm" to radioKm
                    ),
                    "total" to incidentes.size,
                    "incidentes" to incidentes
                )
            )
        } catch (e: Exception) {
            logger.error("Error al buscar incidentes cercanos: ${e.message}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("mensaje" to "Error al buscar incidentes cercanos"))
        }
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