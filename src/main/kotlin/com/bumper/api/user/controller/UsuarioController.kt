package com.bumper.bumper.api
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/users")
class UsuarioController @Autowired constructor(private val usuarioService: UsuarioService) {

    @PostMapping
    fun createUser(@Valid @RequestBody usuarioData: Usuario): ResponseEntity<Usuario> {
        val nuevoUsuario = usuarioService.crearUsuario(usuarioData)
        return ResponseEntity.ok(nuevoUsuario)
    }

    @PostMapping("/login")
    fun login(@RequestBody credenciales: Map<String, String>): ResponseEntity<Usuario> {
        val mail = credenciales["mail"] ?: throw IllegalArgumentException("Correo requerido")
        val password = credenciales["password"] ?: throw IllegalArgumentException("Password requerido")
        val usuario = usuarioService.iniciarSesion(mail, password)
        return ResponseEntity.ok(usuario)
    }

    @PostMapping("/logout")
    fun logout(@RequestHeader("mail", "token")): ResponseEntity<String> {
        usuarioService.cerrarSesion(mail, token)
        return ResponseEntity.ok("Sesión cerrada")
    }

    @GetMapping("/me")
    fun getUser(@RequestHeader("mail", "token") mail: String): ResponseEntity<Usuario> {
        val usuario = usuarioService.obtenerUsuario(mail, token)
        return ResponseEntity.ok(usuario)
    }

    @PutMapping("/update")
    fun updateUser(@RequestBody usuarioData: Usuario): ResponseEntity<Usuario> {
        if (usuarioData.token != "activo") {
            throw IllegalArgumentException("Usuario no autenticado")
        }
        val usuarioActualizado = usuarioService.actualizarUsuario(usuarioData)
        return ResponseEntity.ok(usuarioActualizado)
    }
}