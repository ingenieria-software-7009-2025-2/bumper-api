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

    /**
     * Endpoint para obtener todos los incidentes registrados en el sistema.
     *
     * @return Retorna una respuesta HTTP encapsulada en un objeto [ResponseEntity]:
     *         - Si la operación es exitosa, retorna un estado HTTP 200 (OK) junto con una lista de todos
     *           los incidentes registrados en el cuerpo de la respuesta.
     *         - Si no hay incidentes registrados, la lista estará vacía, pero el estado seguirá siendo 200 (OK).
     */
    @GetMapping
    fun getAllIncidentes(): ResponseEntity<List<Incidente>> {
        // Registra en el logger el intento de obtener todos los incidentes del sistema.
        logger.info("Obteniendo todos los incidentes")

        // Llama al repositorio `incidenteRepository` para recuperar todos los incidentes almacenados.
        val incidentes = incidenteRepository.findAll()

        // Retorna una respuesta HTTP con estado 200 (OK) y la lista de incidentes en el cuerpo.
        return ResponseEntity.ok(incidentes)
    }


    /**
     * Endpoint para obtener todos los incidentes asociados a un usuario específico.
     *
     * @param usuarioId Representa el ID del usuario cuyos incidentes se desean obtener. Este valor se recibe
     *                  como parte de la URL gracias a la anotación `@PathVariable`.
     *
     * @return Retorna una respuesta HTTP encapsulada en un objeto [ResponseEntity]:
     *         - Si la operación es exitosa, retorna un estado HTTP 200 (OK) junto con una lista de incidentes
     *           asociados al usuario en el cuerpo de la respuesta.
     *         - Si no hay incidentes asociados al usuario, la lista estará vacía, pero el estado seguirá siendo 200 (OK).
     */
    @GetMapping("/usuario/{usuarioId}")
    fun getIncidentesByUsuario(@PathVariable usuarioId: Long): ResponseEntity<List<Incidente>> {
        // Registra en el logger el intento de obtener los incidentes del usuario con el ID proporcionado.
        logger.info("Obteniendo incidentes para el usuario con ID: $usuarioId")

        // Llama al repositorio `incidenteRepository` para buscar todos los incidentes asociados al usuario.
        val incidentes = incidenteRepository.findByUsuarioId(usuarioId)

        // Verifica si la lista de incidentes está vacía y registra una advertencia en el logger si es así.
        if (incidentes.isEmpty()) {
            logger.warn("No se encontraron incidentes para el usuario con ID: $usuarioId")
        }

        // Retorna una respuesta HTTP con estado 200 (OK) y la lista de incidentes en el cuerpo.
        return ResponseEntity.ok(incidentes)
    }

    /**
     * Endpoint para crear un nuevo incidente en el sistema.
     *
     * @param incidenteRequest Representa los datos necesarios para crear un incidente. Este objeto se recibe
     *                         en el cuerpo de la solicitud HTTP gracias a la anotación `@RequestBody`.
     *                         Contiene información como el ID del usuario, el tipo de incidente, la ubicación,
     *                         y el tipo de vialidad asociada al incidente.
     *
     * @return Retorna una respuesta HTTP encapsulada en un objeto [ResponseEntity]:
     *         - Si el incidente se crea exitosamente, retorna un estado HTTP 201 (CREATED) junto con el
     *           objeto del incidente creado en el cuerpo de la respuesta.
     *         - Si el usuario no es encontrado, retorna un estado HTTP 404 (NOT FOUND) con un mensaje
     *           indicando que el usuario no fue encontrado.
     *         - Si ocurre un error inesperado durante la creación del incidente, retorna un estado HTTP 500
     *           (INTERNAL SERVER ERROR) con un mensaje genérico de error.
     *
     * @throws IllegalArgumentException Si el usuario no existe, se lanza una excepción de tipo
     *                                  [IllegalArgumentException], que es capturada y manejada adecuadamente.
     */
    @PostMapping
    fun createIncidente(@RequestBody incidenteRequest: IncidenteRequest): ResponseEntity<Any> {
        // Registra en el logger el intento de crear un nuevo incidente para el usuario con el ID proporcionado.
        logger.info("Creando nuevo incidente para usuario con ID: ${incidenteRequest.usuarioId}")

        return try {
            // Busca al usuario en el sistema utilizando su ID.
            val usuario = usuarioRepository.findById(incidenteRequest.usuarioId)
                ?: throw IllegalArgumentException("Usuario no encontrado con ID: ${incidenteRequest.usuarioId}")

            // Crea un nuevo objeto de tipo `Incidente` con los datos proporcionados en la solicitud.
            val incidente = Incidente(
                usuario = usuario,
                tipoIncidente = incidenteRequest.tipoIncidente,
                ubicacion = incidenteRequest.ubicacion,
                horaIncidente = LocalDateTime.now(), // La hora del incidente se registra automáticamente.
                tipoVialidad = incidenteRequest.tipoVialidad
            )

            // Guarda el incidente en el repositorio y obtiene el objeto guardado.
            val savedIncidente = incidenteRepository.save(incidente)

            // Actualiza el contador de incidentes del usuario incrementando el valor actual en 1.
            val usuarioActualizado = usuario.copy(numeroIncidentes = usuario.numeroIncidentes + 1)
            usuarioRepository.update(usuarioActualizado)

            // Registra en el logger el éxito de la creación del incidente.
            logger.info("Incidente creado con éxito, ID: ${savedIncidente.id}")

            // Retorna una respuesta HTTP con estado 201 (CREATED) y el incidente creado en el cuerpo.
            ResponseEntity.status(HttpStatus.CREATED).body(savedIncidente)
        } catch (e: IllegalArgumentException) {
            // Registra en el logger el error ocurrido si el usuario no es encontrado.
            logger.error("Error al crear incidente: ${e.message}", e)

            // Retorna una respuesta HTTP con estado 404 (NOT FOUND) y un mensaje indicando que el usuario no fue encontrado.
            ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado")
        } catch (e: Exception) {
            // Registra en el logger cualquier error inesperado durante la creación del incidente.
            logger.error("Error inesperado al crear incidente: ${e.message}", e)

            // Retorna una respuesta HTTP con estado 500 (INTERNAL SERVER ERROR) y un mensaje genérico de error.
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