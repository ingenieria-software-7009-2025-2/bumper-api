package com.bumper.api.user.controller

import com.bumper.api.user.domain.Usuario
import com.bumper.api.user.service.UsuarioService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/users")
class UsuarioController(private val usuarioService: UsuarioService) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // Expresión regular para validar correo electrónico
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@(.+)$")

    @PostMapping("/create")
    fun registrarUsuario(@RequestBody usuarioRequest: UsuarioRequest): ResponseEntity<Any> {
        logger.info("Registrando nuevo usuario con correo: ${usuarioRequest.correo}")

        return try {
            if (!usuarioRequest.correo.matches(emailRegex)) {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("mensaje" to "Correo electrónico inválido"))
            }

            val usuario = Usuario(
                nombre = usuarioRequest.nombre,
                apellido = usuarioRequest.apellido,
                correo = usuarioRequest.correo,
                password = usuarioRequest.password,
                token = Usuario.TOKEN_INACTIVO  // Usar constante
            )

            val usuarioGuardado = usuarioService.registrarUsuario(usuario)
            ResponseEntity.status(HttpStatus.CREATED).body(
                mapOf(
                    "mensaje" to "Usuario registrado exitosamente",
                    "usuario" to usuarioGuardado.toResponseMap()
                )
            )
        } catch (e: IllegalStateException) {
            logger.warn("Error de validación al registrar usuario: ${e.message}")
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("mensaje" to (e.message ?: "Error de validación")))
        } catch (e: Exception) {
            logger.error("Error al registrar usuario: ${e.message}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("mensaje" to "Error interno al registrar usuario"))
        }
    }

    @GetMapping("/correo/{correo}")
    fun obtenerPorCorreo(@PathVariable correo: String): ResponseEntity<Any> {
        logger.info("Buscando usuario por correo: $correo")
        return try {
            usuarioService.buscarPorCorreo(correo)?.let { usuario ->
                ResponseEntity.ok(mapOf(
                    "mensaje" to "Usuario encontrado",
                    "usuario" to usuario.toResponseMap()
                ))
            } ?: ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(mapOf("mensaje" to "Usuario no encontrado"))
        } catch (e: Exception) {
            logger.error("Error al buscar usuario por correo: ${e.message}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("mensaje" to "Error al buscar usuario"))
        }
    }

    @GetMapping("/{id}")
    fun obtenerPorId(@PathVariable id: Long): ResponseEntity<Any> {
        logger.info("Buscando usuario por ID: $id")
        return try {
            usuarioService.buscarPorId(id)?.let { usuario ->
                ResponseEntity.ok(mapOf(
                    "mensaje" to "Usuario encontrado",
                    "usuario" to usuario.toResponseMap()
                ))
            } ?: ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(mapOf("mensaje" to "Usuario no encontrado"))
        } catch (e: Exception) {
            logger.error("Error al buscar usuario por ID: ${e.message}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("mensaje" to "Error al buscar usuario"))
        }
    }

    @PostMapping("/login")
    fun login(@RequestBody loginRequest: LoginRequest): ResponseEntity<Any> {
        logger.info("Intento de login para usuario: ${loginRequest.correo}")
        return try {
            val usuario = usuarioService.validarCredenciales(
                loginRequest.correo,
                loginRequest.password
            )
            ResponseEntity.ok(mapOf(
                "mensaje" to "Login exitoso",
                "usuario" to usuario.toResponseMap()
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("mensaje" to (e.message ?: "Credenciales inválidas")))
        } catch (e: Exception) {
            logger.error("Error en login: ${e.message}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("mensaje" to "Error en el servidor"))
        }
    }

    @PostMapping("/logout")
    fun logout(@RequestBody logoutRequest: LogoutRequest): ResponseEntity<Any> {
        logger.info("Cerrando sesión para usuario: ${logoutRequest.correo}")
        return try {
            if (usuarioService.cerrarSesion(logoutRequest.correo)) {
                ResponseEntity.ok(mapOf("mensaje" to "Sesión cerrada correctamente"))
            } else {
                ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(mapOf("mensaje" to "Usuario no encontrado"))
            }
        } catch (e: Exception) {
            logger.error("Error al cerrar sesión: ${e.message}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("mensaje" to "Error al cerrar sesión"))
        }
    }

    @PutMapping
    fun actualizarUsuario(@RequestBody request: UsuarioUpdateRequest): ResponseEntity<Any> {
        logger.info("Actualizando usuario con ID: ${request.id}")
        return try {
            if (!request.correo.matches(emailRegex)) {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("mensaje" to "Correo electrónico inválido"))
            }

            val usuario = Usuario(
                id = request.id,
                nombre = request.nombre,
                apellido = request.apellido,
                correo = request.correo,
                password = request.password,
                token = request.token,
                numeroIncidentes = request.numeroIncidentes
            )

            val usuarioActualizado = usuarioService.actualizarUsuario(usuario)
            ResponseEntity.ok(mapOf(
                "mensaje" to "Usuario actualizado correctamente",
                "usuario" to usuarioActualizado.toResponseMap()
            ))
        } catch (e: IllegalStateException) {
            logger.warn("Error de validación al actualizar usuario: ${e.message}")
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("mensaje" to (e.message ?: "Error de validación")))
        } catch (e: Exception) {
            logger.error("Error al actualizar usuario: ${e.message}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("mensaje" to "Error al actualizar usuario"))
        }
    }

    // Función de extensión para mapear Usuario a respuesta
    private fun Usuario.toResponseMap() = mapOf(
        "id" to id,
        "nombre" to nombre,
        "apellido" to apellido,
        "correo" to correo,
        "token" to token,
        "numeroIncidentes" to numeroIncidentes,
        "fechaRegistro" to fechaRegistro
    )
}

// DTOs
data class UsuarioRequest(
    val nombre: String,
    val apellido: String,
    val correo: String,
    val password: String
)

data class LoginRequest(
    val correo: String,
    val password: String
)

data class LogoutRequest(
    val correo: String
)

data class UsuarioUpdateRequest(
    val id: Long,
    val nombre: String,
    val apellido: String,
    val correo: String,
    val password: String,
    val token: String,
    val numeroIncidentes: Int
)