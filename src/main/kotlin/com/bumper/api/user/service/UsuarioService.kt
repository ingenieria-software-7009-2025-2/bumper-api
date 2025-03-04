package com.bumper.api.user.service

import com.bumper.api.user.domain.Usuario
import com.bumper.api.user.repository.UsuarioRepository
import org.springframework.stereotype.Service

@Service
class UsuarioService(private val usuarioRepository: UsuarioRepository) {

    // Crear un nuevo usuario
    fun crearUsuario(usuario: Usuario): Usuario {
        return usuarioRepository.save(usuario)
    }

    // Iniciar sesión
    fun iniciarSesion(mail: String, password: String): Usuario {
        val usuario = usuarioRepository.findByMail(mail)
            ?: throw IllegalArgumentException("Usuario no encontrado")
        if (usuario.password != password) {
            throw IllegalArgumentException("Credenciales incorrectas")
        }
        usuarioRepository.updateToken(mail, "activo")
        return usuario.copy(token = "activo")
    }

    // Cerrar sesión
    fun cerrarSesion(mail: String) {
        usuarioRepository.updateToken(mail, "inactivo")
    }

    // Obtener información de un usuario
    fun obtenerUsuario(mail: String): Usuario {
        return usuarioRepository.findByMail(mail)
            ?: throw IllegalArgumentException("Usuario no encontrado")
    }

    // Actualizar información de un usuario
    fun actualizarUsuario(usuario: Usuario): Usuario {
        return usuarioRepository.save(usuario)
    }
}