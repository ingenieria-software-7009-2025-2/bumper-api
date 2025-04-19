package com.bumper.api.user.controller

import com.bumper.api.user.domain.Usuario
import com.bumper.api.user.service.UsuarioService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/users")
class UsuarioController(private val usuarioService: UsuarioService) {

    private val logger: Logger = LoggerFactory.getLogger(UsuarioController::class.java)

    /**
     * Endpoint para registrar un nuevo usuario.
     */
    @PostMapping("/create")
    fun registrarUsuario(@RequestBody usuarioRequest: UsuarioRequest): ResponseEntity<Any> {
        logger.info("Registrando nuevo usuario con correo: ${usuarioRequest.correo}")

        return try {
            // Validación básica
            if (!usuarioRequest.correo.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)$"))) {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("mensaje" to "Correo electrónico inválido"))
            }

            val usuario = Usuario(
                nombre = usuarioRequest.nombre,
                apellido = usuarioRequest.apellido,
                correo = usuarioRequest.correo,
                password = usuarioRequest.password
            )

            val usuarioGuardado = usuarioService.registrarUsuario(usuario)
            ResponseEntity.status(HttpStatus.CREATED).body(
                mapOf(
                    "mensaje" to "Usuario registrado exitosamente",
                    "usuario" to mapOf(
                        "id" to usuarioGuardado.id,
                        "nombre" to usuarioGuardado.nombre,
                        "apellido" to usuarioGuardado.apellido,
                        "correo" to usuarioGuardado.correo
                    )
                )
            )
        } catch (e: IllegalStateException) {
            logger.warn("Error de validación al registrar usuario: ${e.message}")
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("mensaje" to e.message))
        } catch (e: Exception) {
            logger.error("Error al registrar usuario: ${e.message}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("mensaje" to "Error interno al registrar usuario"))
        }
    }

    /**
     * Endpoint para obtener un usuario por su correo.
     */
    @GetMapping("/correo/{correo}")
    fun obtenerPorCorreo(@PathVariable correo: String): ResponseEntity<Any> {
        logger.info("Buscando usuario por correo: $correo")
        return try {
            val usuario = usuarioService.buscarPorCorreo(correo)
            if (usuario != null) {
                ResponseEntity.ok(mapOf(
                    "mensaje" to "Usuario encontrado",
                    "usuario" to mapOf(
                        "id" to usuario.id,
                        "nombre" to usuario.nombre,
                        "apellido" to usuario.apellido,
                        "correo" to usuario.correo,
                        "numeroIncidentes" to usuario.numeroIncidentes,
                        "fechaRegistro" to usuario.fechaRegistro
                    )
                ))
            } else {
                ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(mapOf("mensaje" to "Usuario no encontrado"))
            }
        } catch (e: Exception) {
            logger.error("Error al buscar usuario por correo: ${e.message}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("mensaje" to "Error al buscar usuario"))
        }
    }

    /**
     * Endpoint para obtener un usuario por su ID.
     */
    @GetMapping("/{id}")
    fun obtenerPorId(@PathVariable id: Long): ResponseEntity<Any> {
        logger.info("Buscando usuario por ID: $id")
        return try {
            val usuario = usuarioService.buscarPorId(id)
            if (usuario != null) {
                ResponseEntity.ok(mapOf(
                    "mensaje" to "Usuario encontrado",
                    "usuario" to mapOf(
                        "id" to usuario.id,
                        "nombre" to usuario.nombre,
                        "apellido" to usuario.apellido,
                        "correo" to usuario.correo,
                        "numeroIncidentes" to usuario.numeroIncidentes,
                        "fechaRegistro" to usuario.fechaRegistro
                    )
                ))
            } else {
                ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(mapOf("mensaje" to "Usuario no encontrado"))
            }
        } catch (e: Exception) {
            logger.error("Error al buscar usuario por ID: ${e.message}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("mensaje" to "Error al buscar usuario"))
        }
    }

    /**
     * Endpoint para actualizar el token de sesión del usuario.
     */
    @PutMapping("/token")
    fun actualizarToken(@RequestBody tokenRequest: TokenRequest): ResponseEntity<Any> {
        logger.info("Actualizando token para usuario: ${tokenRequest.correo}")
        return try {
            val actualizado = usuarioService.actualizarToken(tokenRequest.correo, tokenRequest.token)
            if (actualizado) {
                ResponseEntity.ok(mapOf(
                    "mensaje" to "Token actualizado correctamente",
                    "correo" to tokenRequest.correo
                ))
            } else {
                ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(mapOf("mensaje" to "No se pudo actualizar el token. Usuario no encontrado"))
            }
        } catch (e: Exception) {
            logger.error("Error al actualizar token: ${e.message}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("mensaje" to "Error al actualizar token"))
        }
    }

    /**
     * Endpoint para actualizar los datos de un usuario.
     */
    @PutMapping
    fun actualizarUsuario(@RequestBody usuarioRequest: UsuarioUpdateRequest): ResponseEntity<Any> {
        logger.info("Actualizando usuario con ID: ${usuarioRequest.id}")
        return try {
            // Validación básica del correo
            if (!usuarioRequest.correo.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)$"))) {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("mensaje" to "Correo electrónico inválido"))
            }

            val usuario = Usuario(
                id = usuarioRequest.id,
                nombre = usuarioRequest.nombre,
                apellido = usuarioRequest.apellido,
                correo = usuarioRequest.correo,
                password = usuarioRequest.password,
                token = usuarioRequest.token,
                numeroIncidentes = usuarioRequest.numeroIncidentes
            )

            val usuarioActualizado = usuarioService.actualizarUsuario(usuario)
            ResponseEntity.ok(mapOf(
                "mensaje" to "Usuario actualizado correctamente",
                "usuario" to mapOf(
                    "id" to usuarioActualizado.id,
                    "nombre" to usuarioActualizado.nombre,
                    "apellido" to usuarioActualizado.apellido,
                    "correo" to usuarioActualizado.correo,
                    "numeroIncidentes" to usuarioActualizado.numeroIncidentes,
                    "fechaRegistro" to usuarioActualizado.fechaRegistro
                )
            ))
        } catch (e: IllegalStateException) {
            logger.warn("Error de validación al actualizar usuario: ${e.message}")
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("mensaje" to e.message))
        } catch (e: Exception) {
            logger.error("Error al actualizar usuario: ${e.message}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("mensaje" to "Error al actualizar usuario"))
        }
    }
}

// Los DTOs permanecen igual

/**
 * DTO para registrar usuario.
 */
data class UsuarioRequest(
    val nombre: String,
    val apellido: String,
    val correo: String,
    val password: String
)

/**
 * DTO para actualizar token.
 */
data class TokenRequest(
    val correo: String,
    val token: String
)

/**
 * DTO para actualizar usuario.
 */
data class UsuarioUpdateRequest(
    val id: Long,
    val nombre: String,
    val apellido: String,
    val correo: String,
    val password: String,
    val token: String,
    val numeroIncidentes: Int
)