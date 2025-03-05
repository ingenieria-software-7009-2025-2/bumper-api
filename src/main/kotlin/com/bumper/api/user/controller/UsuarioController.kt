package com.bumper.api.user.controller

import com.bumper.api.user.domain.Usuario
import com.bumper.api.user.service.UsuarioService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/users")
class UsuarioController(private val usuarioService: UsuarioService) {

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

        if (mail.isNullOrBlank() || password.isNullOrBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Correo y contraseña son requeridos")
        }

        return try {
            val usuario = usuarioService.iniciarSesion(mail, password)
            ResponseEntity.ok(usuario)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales inválidas")
        }
    }

    @PostMapping("/logout")
    fun cerrarSesion(@RequestHeader("mail") mail: String): ResponseEntity<String> {
        usuarioService.cerrarSesion(mail)
        return ResponseEntity.ok("Sesión cerrada correctamente")
    }

    @GetMapping("/me")
    fun obtenerUsuario(@RequestHeader("mail") mail: String): ResponseEntity<Any> {
        val usuario = usuarioService.obtenerUsuario(mail)
        return if (usuario != null) {
            ResponseEntity.ok(usuario)
        } else {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado")
        }
    }

    @PutMapping("/update")
    fun actualizarUsuario(@RequestBody usuarioData: Usuario): ResponseEntity<Any> {
        return if (usuarioData.token == "activo") {
            val usuarioActualizado = usuarioService.actualizarUsuario(usuarioData)
            ResponseEntity.ok(usuarioActualizado)
        } else {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado")
        }
    }
}
