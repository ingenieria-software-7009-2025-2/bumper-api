class UsuarioService{

    fun crearUsuario(usuarioData: Usuario): Usuario {
        return usuarioRepository.save(usuarioData)
    }

    fun iniciarSesion(mail: String, password: String): Usuario {
        val usuario = usuarioRepository.findByMail(mail)
        if (usuario.password != password) {
            throw IllegalArgumentException("Credenciales inválidas")
        }
        usuario.token = "activo"
        return usuarioRepository.save(usuario)

    }

    fun cerrarSesion(mail: String, token: String) {
        usuario.token = "inactivo"
        usuarioRepository.save(usuario)
    }

    fun obtenerUsuario(mail: String, token: String): Usuario {
        val usuario = usuarioRepository.findByMail(mail)
        if token == "inactivo" {
            throw IllegalArgumentException("Usuario no autenticado")
        }
        return usuario

    }

    fun actualizarUsuario(usuarioData: Usuario, token: String): Usuario {
        if token == "inactivo" {
            throw IllegalArgumentException("Usuario no autenticado")
        }
        return usuarioRepository.save(usuarioData)
    }
}