package com.bumper.api.user.controller

import com.bumper.api.user.domain.FotoIncidente
import com.bumper.api.user.service.FotoIncidenteService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/fotos-incidentes")
class FotoIncidenteController(
    private val fotoIncidenteService: FotoIncidenteService
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun agregarFoto(@RequestBody fotoRequest: FotoRequest): ResponseEntity<Any> {
        logger.info("Agregando nueva foto para incidente ID: ${fotoRequest.incidenteId}")

        return try {
            val foto = fotoIncidenteService.agregarFoto(
                incidenteId = fotoRequest.incidenteId,
                urlFoto = fotoRequest.urlFoto,
                descripcion = fotoRequest.descripcion
            )

            ResponseEntity.status(HttpStatus.CREATED).body(
                mapOf(
                    "mensaje" to "Foto agregada exitosamente",
                    "fotoId" to foto.id
                )
            )
        } catch (e: IllegalArgumentException) {
            logger.error("Error al agregar foto: ${e.message}", e)
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.message)
        } catch (e: Exception) {
            logger.error("Error inesperado al agregar foto: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al agregar la foto")
        }
    }

    /**
     * Endpoint para obtener todas las fotos de un incidente específico.
     */
    @GetMapping("/incidente/{incidenteId}")
    fun obtenerFotosPorIncidente(@PathVariable incidenteId: String): ResponseEntity<List<FotoIncidente>> {
        logger.info("Obteniendo fotos para incidente ID: $incidenteId")
        val fotos = fotoIncidenteService.getFotosDeIncidente(incidenteId)
        return ResponseEntity.ok(fotos)
    }

    /**
     * Endpoint para eliminar una foto específica.
     */
    @DeleteMapping("/{id}")
    fun eliminarFoto(
        @PathVariable id: Long,
        @RequestParam usuarioId: Long
    ): ResponseEntity<Any> {
        logger.info("Eliminando foto ID: $id (solicitado por usuario ID: $usuarioId)")

        return try {
            fotoIncidenteService.eliminarFoto(id, usuarioId)
            logger.info("Foto eliminada exitosamente")
            ResponseEntity.ok("Foto eliminada exitosamente")
        } catch (e: IllegalArgumentException) {
            logger.error("Permiso denegado o foto no encontrada: ${e.message}", e)
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.message)
        } catch (e: Exception) {
            logger.error("Error al eliminar foto: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al eliminar la foto")
        }
    }

    /**
     * Endpoint para actualizar la descripción de una foto.
     */
    @PutMapping("/{id}/descripcion")
    fun actualizarDescripcion(
        @PathVariable id: Long,
        @RequestParam usuarioId: Long,
        @RequestBody descripcionRequest: DescripcionRequest
    ): ResponseEntity<Any> {
        logger.info("Actualizando descripción de foto ID: $id")

        return try {
            fotoIncidenteService.actualizarDescripcion(id, descripcionRequest.descripcion, usuarioId)
            ResponseEntity.ok("Descripción actualizada exitosamente")
        } catch (e: IllegalArgumentException) {
            logger.error("Permiso denegado o foto no encontrada: ${e.message}", e)
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.message)
        } catch (e: Exception) {
            logger.error("Error al actualizar descripción: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al actualizar la descripción")
        }
    }
}

/**
 * DTO para la solicitud de agregar una foto.
 */
data class FotoRequest(
    val incidenteId: String,
    val urlFoto: String,
    val descripcion: String? = null
)

/**
 * DTO para actualizar la descripción de una foto.
 */
data class DescripcionRequest(
    val descripcion: String
)