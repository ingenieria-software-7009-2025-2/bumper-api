package com.bumper.api.user.controller

import com.bumper.api.user.domain.Incidente
import com.bumper.api.user.repository.IncidenteRepository
import com.bumper.api.user.repository.UsuarioRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/v1/incidentes")
class IncidenteController(
    private val incidenteRepository: IncidenteRepository,
    private val usuarioRepository: UsuarioRepository
) {

    private val logger: Logger = LoggerFactory.getLogger(IncidenteController::class.java)

    // Obtener todos los incidentes
    @GetMapping
    fun getAllIncidentes(): ResponseEntity<List<Incidente>> {
        logger.info("Obteniendo todos los incidentes")
        val incidentes = incidenteRepository.findAll()
        return ResponseEntity.ok(incidentes)
    }

    // Obtener incidentes por usuario
    @GetMapping("/usuario/{usuarioId}")
    fun getIncidentesByUsuario(@PathVariable usuarioId: Long): ResponseEntity<List<Incidente>> {
        logger.info("Obteniendo incidentes para el usuario con ID: $usuarioId")
        val incidentes = incidenteRepository.findByUsuarioId(usuarioId)
        if (incidentes.isEmpty()) {
            logger.warn("No se encontraron incidentes para el usuario con ID: $usuarioId")
        }
        return ResponseEntity.ok(incidentes)
    }

    // Crear un nuevo incidente
    @PostMapping
    fun createIncidente(@RequestBody incidenteRequest: IncidenteRequest): ResponseEntity<Any> {
        logger.info("Creando nuevo incidente para usuario con ID: ${incidenteRequest.usuarioId}")
        return try {
            // Buscar usuario por ID (necesitamos agregar este método al repositorio si no existe)
            val usuario = usuarioRepository.findById(incidenteRequest.usuarioId)
                ?: throw IllegalArgumentException("Usuario no encontrado con ID: ${incidenteRequest.usuarioId}")

            val incidente = Incidente(
                usuario = usuario,
                tipoIncidente = incidenteRequest.tipoIncidente,
                ubicacion = incidenteRequest.ubicacion,
                horaIncidente = LocalDateTime.now(),
                tipoVialidad = incidenteRequest.tipoVialidad
            )

            val savedIncidente = incidenteRepository.save(incidente)

            // Actualizar el contador de incidentes del usuario
            val usuarioActualizado = usuario.copy(numeroIncidentes = usuario.numeroIncidentes + 1)
            usuarioRepository.update(usuarioActualizado)

            logger.info("Incidente creado con éxito, ID: ${savedIncidente.id}")
            ResponseEntity.status(HttpStatus.CREATED).body(savedIncidente)
        } catch (e: IllegalArgumentException) {
            logger.error("Error al crear incidente: ${e.message}", e)
            ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado")
        } catch (e: Exception) {
            logger.error("Error inesperado al crear incidente: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al crear el incidente")
        }
    }
}

// DTO para la solicitud de creación de incidente
data class IncidenteRequest(
    val usuarioId: Long,
    val tipoIncidente: String,
    val ubicacion: String,
    val tipoVialidad: String
)