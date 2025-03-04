package com.bumper.api.user.controller

import com.bumper.api.user.domain.Usuario
import com.bumper.api.user.service.UsuarioService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/users")
class UsuarioController(private val usuarioService: UsuarioService) {

    @GetMapping("/hello")
    fun hello(): String {
        return "¡Hola desde el controlador de usuarios!"
    }

    // Crear un nuevo usuario
    @PostMapping("/create")
    fun createUser(@RequestBody usuarioData: Usuario): ResponseEntity<Usuario> {
        val nuevoUsuario = usuarioService.crearUsuario(usuarioData)
        return ResponseEntity.ok(nuevoUsuario)
    }

    // Iniciar sesión
    @PostMapping("/login")
    fun iniciarSesion(@RequestBody credenciales: Map<String, String>): ResponseEntity<Usuario> {
        val mail = credenciales["mail"] ?: throw IllegalArgumentException("Correo requerido")
        val password = credenciales["password"] ?: throw IllegalArgumentException("Password requerido")
        val usuario = usuarioService.iniciarSesion(mail, password)
        return ResponseEntity.ok(usuario)
    }

    // Cerrar sesión
    @PostMapping("/logout")
    fun cerrarSesion(@RequestHeader("mail") mail: String): ResponseEntity<String> {
        usuarioService.cerrarSesion(mail)
        return ResponseEntity.ok("Sesión cerrada")
    }

    // Obtener información de un usuario
    @GetMapping("/me")
    fun obtenerUsuario(@RequestHeader("mail") mail: String): ResponseEntity<Usuario> {
        val usuario = usuarioService.obtenerUsuario(mail)
        return ResponseEntity.ok(usuario)
    }

    // Actualizar información de un usuario
    @PutMapping("/update")
    fun actualizarUsuario(@RequestBody usuarioData: Usuario): ResponseEntity<Usuario> {
        if (usuarioData.token != "activo") {
            throw IllegalArgumentException("Usuario no autenticado")
        }
        val usuarioActualizado = usuarioService.actualizarUsuario(usuarioData)
        return ResponseEntity.ok(usuarioActualizado)
    }
}