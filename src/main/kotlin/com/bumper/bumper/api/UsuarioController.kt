package com.bumper.bumper.api
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/users")
class UsuarioController {
    //Todo esto fue realmente un codigo a las prisas ya que necesitaba entregar al menos algo
    //Esto no es un codigo que se deberia de usar en produccion
    //Se reestructurara en el futuro, por ahora solo es un prototipo
    //Metodo POST para crear un usuario
    @PostMapping
    fun createUser(@RequestBody usuarioData: Usuario): ResponseEntity<Usuario> {

        val nuevoUsuario = Usuario(
            mail = usuarioData.mail,
            nombre = usuarioData.nombre,
            apellido = usuarioData.apellido,
            password = usuarioData.password,
            token = usuarioData.token,)
        //Las llamadas a base de datos se realizaran en un documento aparte pero por ahora pondremos que llamadas se utilizaran.
        query = "INSERT INTO usuarios (mail, nombre, apellido, password, token) VALUES ('${nuevoUsuario.mail}', '${nuevoUsuario.nombre}', '${nuevoUsuario.apellido}', '${nuevoUsuario.password}', '${nuevoUsuario.token}')"
        db.execute(query)
        return ResponseEntity.ok(nuevoUsuario)
    }

    // Metodo POST para iniciar sesion
    @PostMapping("/login")
    fun iniciarSesion(@RequestBody credenciales: Map<String, String>): ResponseEntity<Usuario> {
        val mail = credenciales["mail"] ?: throw IllegalArgumentException("correo requerido")
        val password = credenciales["password"] ?: throw IllegalArgumentException("Password requerido")
        //Las llamadas a base de datos se realizaran de esta manera por ahora
        query = "SELECT password FROM usuarios WHERE mail = '$mail'"
        info = db.execute(query)
        if (info.password != password) {
            throw IllegalArgumentException("Credenciales incorrectas")
        } else {
            query = "UPDATE usuarios SET token = 'activo' WHERE mail = '$mail'"
            db.execute(query)
            token = "activo"
        }


        val usuario = Usuario(mail, password, token)
        return ResponseEntity.ok(usuario)
    }

    // Metodo POST para cerrar sesion
    @PostMapping("/logout")
    fun cerrarSesion(): ResponseEntity<String> {
        //Esto es una medida temporal ya que el objetivo es que el token sea unico y esto no permitiria multiples sesiones
        query = "UPDATE usuarios SET token = 'inactivo' WHERE token = 'activo'"
        return ResponseEntity.ok("sesion cerrada")
    }

    // Metodo GET para obtener informacion de un usuario
    @GetMapping("/me")
    fun obtenerUsuario(): ResponseEntity<Usuario> {
        query = "SELECT * FROM usuarios WHERE token = 'activo'"
        return ResponseEntity.ok(usuario)
    }

    // Metodo PUT para actualizar informacion de un usuario autenticado
    @PutMapping("/update")
    fun actualizarUsuario(@RequestBody usuarioData: Usuario): ResponseEntity<Usuario> {
        if (usuarioData.token != "activo") {
            throw IllegalArgumentException("Usuario no autenticado")
        }
        query = "UPDATE usuarios SET nombre = '${usuarioData.nombre}', apellido = '${usuarioData.apellido}', password = '${usuarioData.password}' WHERE mail = '${usuarioData.mail}'"
        db.execute(query)
        return ResponseEntity.ok(usuario)
    }
}
