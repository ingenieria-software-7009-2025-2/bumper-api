package com.bumper.api.user.service

import com.bumper.api.user.domain.Usuario
import com.bumper.api.user.repository.UsuarioRepository
import org.springframework.stereotype.Service

@Service
class UsuarioService(private val usuarioRepository: UsuarioRepository) {

    // Crear un nuevo usuario
    fun crearUsuario(usuario: Usuario): Usuario {
        // Validar datos antes de guardar (opcional)
        require(usuario.correo.isNotBlank()) { "El correo no puede estar vacío" }
        require(usuario.nombre.isNotBlank()) { "El nombre no puede estar vacío" }
        require(usuario.apellido.isNotBlank()) { "El apellido no puede estar vacío" }
        require(usuario.password.isNotBlank()) { "La contraseña no puede estar vacía" }

        return usuarioRepository.save(usuario)
    }

    // Iniciar sesión
    fun iniciarSesion(correo: String, password: String): Usuario {
        val usuario = usuarioRepository.findByCorreo(correo)
            ?: throw IllegalArgumentException("Usuario no encontrado")
        if (usuario.password != password) {
            throw IllegalArgumentException("Credenciales incorrectas")
        }
        usuarioRepository.updateToken(correo, "activo")
        return usuario.copy(token = "activo")
    }

    // Cerrar sesión
    fun cerrarSesion(correo: String) {
        val usuario = usuarioRepository.findByCorreo(correo)
            ?: throw IllegalArgumentException("Usuario no encontrado")
        usuarioRepository.updateToken(correo, "inactivo")
    }

    // Obtener información de un usuario
    fun obtenerUsuario(correo: String): Usuario? {
        return usuarioRepository.findByCorreo(correo)
    }

    // Actualizar información de un usuario
    fun actualizarUsuario(usuario: Usuario): Usuario {
        val usuarioExistente = usuarioRepository.findByCorreo(usuario.correo)
            ?: throw IllegalArgumentException("Usuario no encontrado")
        // Solo actualizamos campos permitidos, numeroIncidentes se gestiona en otro lugar
        val usuarioActualizado = usuarioExistente.copy(
            nombre = usuario.nombre,
            apellido = usuario.apellido,
            password = usuario.password,
            token = usuario.token
        )
        return usuarioRepository.update(usuarioActualizado)
    }
}