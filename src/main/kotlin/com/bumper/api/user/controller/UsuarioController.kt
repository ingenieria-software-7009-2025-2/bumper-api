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

    @GetMapping("/hello")
    fun hello(): ResponseEntity<String> {
        return ResponseEntity.ok("¡Hola desde el controlador de usuarios!")
    }

    @PostMapping("/create")
    fun createUser(@RequestBody usuarioData: Usuario): ResponseEntity<Usuario> {
        return try {
            logger.info("Creando usuario con correo: ${usuarioData.correo}")
            val nuevoUsuario = usuarioService.crearUsuario(usuarioData)
            ResponseEntity.status(HttpStatus.CREATED).body(nuevoUsuario)
        } catch (e: Exception) {
            logger.error("Error al crear usuario: ${e.message}", e)
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/login")
    fun iniciarSesion(@RequestBody credenciales: Map<String, String>): ResponseEntity<Any> {
        val correo = credenciales["correo"]
        val password = credenciales["password"]
        logger.info("Intentando iniciar sesión con correo: $correo")

        if (correo.isNullOrBlank() || password.isNullOrBlank()) {
            logger.warn("Correo o contraseña vacíos")
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Correo y contraseña son requeridos")
        }

        return try {
            val usuario = usuarioService.iniciarSesion(correo, password)
            logger.info("Inicio de sesión exitoso para el usuario con correo: $correo")
            ResponseEntity.ok(usuario)
        } catch (e: IllegalArgumentException) {
            logger.error("Error en inicio de sesión: ${e.message}", e)
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales inválidas")
        }
    }

    @PostMapping("/logout")
    fun cerrarSesion(@RequestHeader("correo") correo: String): ResponseEntity<String> {
        logger.info("Cerrando sesión para el usuario con correo: $correo")
        return try {
            usuarioService.cerrarSesion(correo)
            ResponseEntity.ok("Sesión cerrada correctamente")
        } catch (e: IllegalArgumentException) {
            logger.error("Error al cerrar sesión: ${e.message}", e)
            ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado")
        }
    }

    @GetMapping("/me")
    fun obtenerUsuario(@RequestHeader("correo") correo: String): ResponseEntity<Any> {
        logger.info("Obteniendo información para el usuario con correo: $correo")
        val usuario = usuarioService.obtenerUsuario(correo)
        return if (usuario != null) {
            logger.info("Usuario encontrado: ${usuario.correo}, incidentes: ${usuario.numeroIncidentes}")
            ResponseEntity.ok(usuario)
        } else {
            logger.warn("Usuario no encontrado con correo: $correo")
            ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado")
        }
    }

    @PutMapping("/update")
    fun actualizarUsuario(
        @RequestHeader("correo") correo: String,
        @RequestBody newData: Usuario
    ): ResponseEntity<Any> {
        logger.info("Actualizando información para el usuario con correo: $correo")
        return try {
            val usuarioExistente = usuarioService.obtenerUsuario(correo)
                ?: throw IllegalArgumentException("Usuario no encontrado")
            val usuarioActualizado = usuarioService.actualizarUsuario(
                usuarioExistente.copy(
                    nombre = newData.nombre,
                    apellido = newData.apellido,
                    password = newData.password
                    // No permitimos actualizar correo ni numeroIncidentes aquí, ya que son gestionados por otros medios
                )
            )
            logger.info("Usuario actualizado con éxito: ${usuarioActualizado.correo}")
            ResponseEntity.ok(usuarioActualizado)
        } catch (e: IllegalArgumentException) {
            logger.error("Error al actualizar usuario: ${e.message}", e)
            ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado")
        }
    }
}