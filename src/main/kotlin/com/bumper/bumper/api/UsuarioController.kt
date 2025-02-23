package com.bumper.bumper.api
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/users")
class UsuarioController {

    //Metodo POST para crear un usuario
    @PostMapping
    fun createUser(@RequestBody usuarioData: Usuario): ResponseEntity<Usuario> {

        val nuevoUsuario = Usuario(
            mail = usuarioData.mail,
            password = usuarioData.password,
            token = usuarioData.token,)
        return ResponseEntity.ok(nuevoUsuario)
    }

    // Metodo POST para iniciar sesion
    @PostMapping("/login")
    fun iniciarSesion(@RequestBody credenciales: Map<String, String>): ResponseEntity<Usuario> {
        val mail = credenciales["mail"] ?: throw IllegalArgumentException("correo requerido")
        val password = credenciales["password"] ?: throw IllegalArgumentException("Password requerido")

        val usuario = Usuario(mail, password, "tokennn")
        return ResponseEntity.ok(usuario)
    }

    // Metodo POST para cerrar sesion
    @PostMapping("/logout")
    fun cerrarSesion(): ResponseEntity<String> {
        return ResponseEntity.ok("sesion cerrada")
    }

    // Metodo GET para obtener informacion de un usuario
    @GetMapping("/me")
    fun obtenerUsuario(): ResponseEntity<Usuario> {
        val usuario = Usuario("usuario@example.com", "password", "tokennn")
        return ResponseEntity.ok(usuario)
    }
}
