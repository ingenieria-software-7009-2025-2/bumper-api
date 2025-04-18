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
            val usuario = Usuario(
                nombre = usuarioRequest.nombre,
                apellido = usuarioRequest.apellido,
                correo = usuarioRequest.correo,
                password = usuarioRequest.password
                // Los demás campos usarán sus valores por defecto
            )

            // Validación básica
            if (!usuario.isValidEmail()) {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Correo electrónico inválido")
            }

            val usuarioGuardado = usuarioService.registrarUsuario(usuario)
            ResponseEntity.status(HttpStatus.CREATED).body(usuarioGuardado)
        } catch (e: Exception) {
            logger.error("Error al registrar usuario: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al registrar usuario")
        }
    }

    /**
     * Endpoint para obtener un usuario por su correo.
     */
    @GetMapping("/correo/{correo}")
    fun obtenerPorCorreo(@PathVariable correo: String): ResponseEntity<Any> {
        logger.info("Buscando usuario por correo: $correo")
        val usuario = usuarioService.buscarPorCorreo(correo)
        return if (usuario != null) {
            ResponseEntity.ok(usuario)
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado")
        }
    }

    /**
     * Endpoint para obtener un usuario por su ID.
     */
    @GetMapping("/{id}")
    fun obtenerPorId(@PathVariable id: Long): ResponseEntity<Any> {
        logger.info("Buscando usuario por ID: $id")
        val usuario = usuarioService.buscarPorId(id)
        return if (usuario != null) {
            ResponseEntity.ok(usuario)
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado")
        }
    }

    /**
     * Endpoint para actualizar el token de sesión del usuario.
     */
    @PutMapping("/token")
    fun actualizarToken(@RequestBody tokenRequest: TokenRequest): ResponseEntity<Any> {
        logger.info("Actualizando token para usuario: ${tokenRequest.correo}")
        return try {
            usuarioService.actualizarToken(tokenRequest.correo, tokenRequest.token)
            ResponseEntity.ok("Token actualizado correctamente")
        } catch (e: Exception) {
            logger.error("Error al actualizar token: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al actualizar token")
        }
    }

    /**
     * Endpoint para actualizar los datos de un usuario.
     */
    @PutMapping
    fun actualizarUsuario(@RequestBody usuarioRequest: UsuarioUpdateRequest): ResponseEntity<Any> {
        logger.info("Actualizando usuario con ID: ${usuarioRequest.id}")
        return try {
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
            ResponseEntity.ok(usuarioActualizado)
        } catch (e: Exception) {
            logger.error("Error al actualizar usuario: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al actualizar usuario")
        }
    }
}

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