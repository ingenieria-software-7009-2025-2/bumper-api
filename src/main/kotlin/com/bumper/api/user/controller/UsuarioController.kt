package com.bumper.api.user.controller

import com.bumper.api.user.domain.Usuario
import com.bumper.api.user.service.UsuarioService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Controlador REST para la gestión de usuarios del sistema Bumper
 * Maneja todas las operaciones CRUD y autenticación de usuarios
 */
@RestController
@RequestMapping("/v1/users")
class UsuarioController(private val usuarioService: UsuarioService) {

    private val logger = LoggerFactory.getLogger(javaClass)

    // Expresión regular para validar formato de correo electrónico
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@(.+)$")

    /**
     * Endpoint para registrar un nuevo usuario en el sistema
     * Valida el formato del correo y delega la creación al servicio
     */
    @PostMapping("/create")
    fun registrarUsuario(@RequestBody usuarioRequest: UsuarioRequest): ResponseEntity<Any> {
        logger.info("Registrando nuevo usuario con correo: ${usuarioRequest.correo}")

        return try {
            // Validación del formato del correo electrónico
            if (!usuarioRequest.correo.matches(emailRegex)) {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("mensaje" to "Correo electrónico inválido"))
            }

            // Construcción del objeto Usuario con token inactivo por defecto
            val usuario = Usuario(
                nombre = usuarioRequest.nombre,
                apellido = usuarioRequest.apellido,
                correo = usuarioRequest.correo,
                password = usuarioRequest.password,
                token = Usuario.TOKEN_INACTIVO
            )

            // Delegación al servicio para persistir el usuario
            val usuarioGuardado = usuarioService.registrarUsuario(usuario)

            // Respuesta exitosa con datos del usuario creado
            ResponseEntity.status(HttpStatus.CREATED).body(
                mapOf(
                    "mensaje" to "Usuario registrado exitosamente",
                    "usuario" to usuarioGuardado.toResponseMap()
                )
            )
        } catch (e: IllegalStateException) {
            // Manejo de errores de validación de negocio
            logger.warn("Error de validación al registrar usuario: ${e.message}")
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("mensaje" to (e.message ?: "Error de validación")))
        } catch (e: Exception) {
            // Manejo de errores inesperados del sistema
            logger.error("Error al registrar usuario: ${e.message}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("mensaje" to "Error interno al registrar usuario"))
        }
    }

    /**
     * Endpoint para buscar un usuario por su dirección de correo electrónico
     * Retorna los datos del usuario si existe, caso contrario error 404
     */
    @GetMapping("/correo/{correo}")
    fun obtenerPorCorreo(@PathVariable correo: String): ResponseEntity<Any> {
        logger.info("Buscando usuario por correo: $correo")

        return try {
            // Búsqueda del usuario por correo usando el servicio
            usuarioService.buscarPorCorreo(correo)?.let { usuario ->
                // Usuario encontrado - retorna datos mapeados
                ResponseEntity.ok(mapOf(
                    "mensaje" to "Usuario encontrado",
                    "usuario" to usuario.toResponseMap()
                ))
            } ?:
            // Usuario no encontrado - retorna error 404
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(mapOf("mensaje" to "Usuario no encontrado"))
        } catch (e: Exception) {
            // Manejo de errores durante la búsqueda
            logger.error("Error al buscar usuario por correo: ${e.message}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("mensaje" to "Error al buscar usuario"))
        }
    }

    /**
     * Endpoint para buscar un usuario por su identificador único
     * Utiliza el ID numérico para localizar el registro en la base de datos
     */
    @GetMapping("/{id}")
    fun obtenerPorId(@PathVariable id: Long): ResponseEntity<Any> {
        logger.info("Buscando usuario por ID: $id")

        return try {
            // Búsqueda del usuario por ID usando el servicio
            usuarioService.buscarPorId(id)?.let { usuario ->
                // Usuario encontrado - retorna datos mapeados
                ResponseEntity.ok(mapOf(
                    "mensaje" to "Usuario encontrado",
                    "usuario" to usuario.toResponseMap()
                ))
            } ?:
            // Usuario no encontrado - retorna error 404
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(mapOf("mensaje" to "Usuario no encontrado"))
        } catch (e: Exception) {
            // Manejo de errores durante la búsqueda
            logger.error("Error al buscar usuario por ID: ${e.message}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("mensaje" to "Error al buscar usuario"))
        }
    }

    /**
     * Endpoint para autenticar un usuario en el sistema
     * Valida credenciales y activa el token de sesión si son correctas
     */
    @PostMapping("/login")
    fun login(@RequestBody loginRequest: LoginRequest): ResponseEntity<Any> {
        logger.info("Intento de login para usuario: ${loginRequest.correo}")

        return try {
            // Validación de credenciales y activación de token
            val usuario = usuarioService.validarCredenciales(
                loginRequest.correo,
                loginRequest.password
            )

            // Login exitoso - retorna datos del usuario autenticado
            ResponseEntity.ok(mapOf(
                "mensaje" to "Login exitoso",
                "usuario" to usuario.toResponseMap()
            ))
        } catch (e: IllegalArgumentException) {
            // Credenciales inválidas - retorna error 401
            ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("mensaje" to (e.message ?: "Credenciales inválidas")))
        } catch (e: Exception) {
            // Error interno durante autenticación
            logger.error("Error en login: ${e.message}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("mensaje" to "Error en el servidor"))
        }
    }

    /**
     * Endpoint para cerrar la sesión de un usuario
     * Desactiva el token de sesión estableciéndolo como inactivo
     */
    @PostMapping("/logout")
    fun logout(@RequestBody logoutRequest: LogoutRequest): ResponseEntity<Any> {
        logger.info("Cerrando sesión para usuario: ${logoutRequest.correo}")

        return try {
            // Desactivación del token de sesión
            if (usuarioService.cerrarSesion(logoutRequest.correo)) {
                // Logout exitoso
                ResponseEntity.ok(mapOf("mensaje" to "Sesión cerrada correctamente"))
            } else {
                // Usuario no encontrado para logout
                ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(mapOf("mensaje" to "Usuario no encontrado"))
            }
        } catch (e: Exception) {
            // Error durante el proceso de logout
            logger.error("Error al cerrar sesión: ${e.message}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("mensaje" to "Error al cerrar sesión"))
        }
    }

    /**
     * Endpoint para actualizar todos los datos de un usuario existente
     * Permite modificar información personal y estado del usuario
     */
    @PutMapping
    fun actualizarUsuario(@RequestBody request: UsuarioUpdateRequest): ResponseEntity<Any> {
        logger.info("Actualizando usuario con ID: ${request.id}")

        return try {
            // Validación del formato del nuevo correo electrónico
            if (!request.correo.matches(emailRegex)) {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("mensaje" to "Correo electrónico inválido"))
            }

            // Construcción del objeto Usuario con todos los datos actualizados
            val usuario = Usuario(
                id = request.id,
                nombre = request.nombre,
                apellido = request.apellido,
                correo = request.correo,
                password = request.password,
                token = request.token,
                numeroIncidentes = request.numeroIncidentes
            )

            // Delegación al servicio para persistir los cambios
            val usuarioActualizado = usuarioService.actualizarUsuario(usuario)

            // Respuesta exitosa con datos actualizados
            ResponseEntity.ok(mapOf(
                "mensaje" to "Usuario actualizado correctamente",
                "usuario" to usuarioActualizado.toResponseMap()
            ))
        } catch (e: IllegalStateException) {
            // Manejo de errores de validación de negocio
            logger.warn("Error de validación al actualizar usuario: ${e.message}")
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("mensaje" to (e.message ?: "Error de validación")))
        } catch (e: Exception) {
            // Manejo de errores inesperados durante actualización
            logger.error("Error al actualizar usuario: ${e.message}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("mensaje" to "Error al actualizar usuario"))
        }
    }

    /**
     * Endpoint especializado para actualizar únicamente la contraseña del usuario
     * Operación más segura que limita la modificación a solo este campo sensible
     */
    @PutMapping("/update-password")
    fun actualizarPassword(@RequestBody request: UpdatePasswordRequest): ResponseEntity<Any> {
        logger.info("Actualizando contraseña para usuario ID: ${request.id}")

        return try {
            // Delegación al servicio para actualizar solo la contraseña
            val actualizado = usuarioService.actualizarPassword(request.id, request.nuevaPassword)

            if (actualizado) {
                // Actualización exitosa de contraseña
                ResponseEntity.ok(mapOf(
                    "mensaje" to "Contraseña actualizada correctamente"
                ))
            } else {
                // Fallo en la actualización (usuario no encontrado o error de BD)
                ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("mensaje" to "No se pudo actualizar la contraseña"))
            }
        } catch (e: IllegalArgumentException) {
            // Errores de validación (contraseña vacía, usuario inexistente)
            logger.warn("Error de validación al actualizar contraseña: ${e.message}")
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(mapOf("mensaje" to (e.message ?: "Error de validación")))
        } catch (e: Exception) {
            // Errores inesperados durante actualización de contraseña
            logger.error("Error al actualizar contraseña: ${e.message}", e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("mensaje" to "Error interno al actualizar contraseña"))
        }
    }

    /**
     * DTO para solicitudes de actualización de contraseña
     * Contiene solo los campos necesarios para esta operación específica
     */
    data class UpdatePasswordRequest(
        val id: Long,
        val nuevaPassword: String
    )

    /**
     * Función de extensión para mapear entidad Usuario a respuesta JSON
     * Excluye campos sensibles como la contraseña del response
     */
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

// ===== DTOs (Data Transfer Objects) =====

/**
 * DTO para solicitudes de registro de nuevo usuario
 * Contiene los campos mínimos requeridos para crear una cuenta
 */
data class UsuarioRequest(
    val nombre: String,
    val apellido: String,
    val correo: String,
    val password: String
)

/**
 * DTO para solicitudes de autenticación
 * Contiene las credenciales necesarias para el login
 */
data class LoginRequest(
    val correo: String,
    val password: String
)

/**
 * DTO para solicitudes de cierre de sesión
 * Identifica al usuario que desea hacer logout
 */
data class LogoutRequest(
    val correo: String
)

/**
 * DTO para solicitudes de actualización completa de usuario
 * Incluye todos los campos modificables del usuario
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