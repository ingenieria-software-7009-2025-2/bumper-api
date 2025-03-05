package com.bumper.api.user.controller

import com.bumper.api.user.domain.Usuario
import com.bumper.api.user.service.UsuarioService
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/users")
class UsuarioController(private val usuarioService: UsuarioService) {

    // Inicializa el logger
    private val logger: Logger = LoggerFactory.getLogger(UsuarioController::class.java)

    @GetMapping("/hello")
    fun hello(): ResponseEntity<String> {
        return ResponseEntity.ok("¡Hola desde el controlador de usuarios!")
    }

    @PostMapping("/create")
    fun createUser(@RequestBody usuarioData: Usuario): ResponseEntity<Usuario> {
        return try {
            val nuevoUsuario = usuarioService.crearUsuario(usuarioData)
            ResponseEntity.status(HttpStatus.CREATED).body(nuevoUsuario)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/login")
    fun iniciarSesion(@RequestBody credenciales: Map<String, String>): ResponseEntity<Any> {
        val mail = credenciales["mail"]
        val password = credenciales["password"]
        logger.info("Intentando iniciar sesión con mail: $mail") // Log para el endpoint login

        if (mail.isNullOrBlank() || password.isNullOrBlank()) {
            logger.warn("Correo o contraseña vacíos") // Log de advertencia
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Correo y contraseña son requeridos")
        }   

        return try {
            val usuario = usuarioService.iniciarSesion(mail, password)
            logger.info("Inicio de sesión exitoso para el usuario: $usuario") // Log de éxito
            ResponseEntity.ok(usuario)
        } catch (e: IllegalArgumentException) {
            logger.error("Error en inicio de sesión: ${e.message}", e) // Log de error
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales inválidas")
        }
    }

    @PostMapping("/logout")
    fun cerrarSesion(@RequestHeader("mail") mail: String): ResponseEntity<String> {
        logger.info("Cerrando sesión para el usuario con mail: $mail") // Log para el endpoint logout
        usuarioService.cerrarSesion(mail)
        return ResponseEntity.ok("Sesión cerrada correctamente")
    }

    @GetMapping("/me")
    fun obtenerUsuario(@RequestHeader("mail") mail: String): ResponseEntity<Any> {
        logger.info("Obteniendo información para el usuario con mail: $mail") // Log para el endpoint me
        val usuario = usuarioService.obtenerUsuario(mail)
        return if (usuario != null) {
            logger.info("Usuario encontrado: $usuario") // Log de éxito
            ResponseEntity.ok(usuario)
        } else {
            logger.warn("Usuario no encontrado") // Log de advertencia
            ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado")
        }
    }

    @PutMapping("/update")
    fun actualizarUsuario(@RequestHeader("mail") mail: String, @RequestBody newData: Usuario): ResponseEntity<Any> {
        logger.info("Actualizando información para el usuario con mail: $mail")
        return try {
            val usuario = usuarioService.obtenerUsuario(mail)
            val usuarioActualizado = usuarioService.actualizarUsuario(usuario.copy(
                nombre = newData.nombre,
                apellido = newData.apellido,
                password = newData.password
            ))
            ResponseEntity.ok(usuarioActualizado)
        } catch (e: IllegalArgumentException) {
            logger.error("Error al actualizar usuario: ${e.message}", e)
            ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado")
        }
    }
}
