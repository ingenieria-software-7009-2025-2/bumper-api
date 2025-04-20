package com.bumper.api.user.service

import com.bumper.api.user.domain.Usuario
import com.bumper.api.user.repository.UsuarioRepository
import org.springframework.stereotype.Service

@Service
class UsuarioService(private val usuarioRepository: UsuarioRepository) {

    /**
     * Registra un nuevo usuario en el sistema.
     */
    fun registrarUsuario(usuario: Usuario): Usuario {
        return usuarioRepository.save(usuario)
    }

    /**
     * Busca un usuario por su correo electrónico.
     */
    fun buscarPorCorreo(correo: String): Usuario? {
        return usuarioRepository.findByCorreo(correo)
    }

    /**
     * Busca un usuario por su ID.
     */
    fun buscarPorId(id: Long): Usuario? {
        return usuarioRepository.findById(id)
    }

    /**
     * Actualiza los datos de un usuario.
     */
    fun actualizarUsuario(usuario: Usuario): Usuario {
        return usuarioRepository.update(usuario)
    }

    fun validarCredenciales(correo: String, password: String): Usuario {
        val usuario = usuarioRepository.findByCorreo(correo)
            ?: throw IllegalArgumentException("Usuario no encontrado")

        if (usuario.password != password) { // En producción usar BCrypt
            throw IllegalArgumentException("Contraseña incorrecta")
        }

        return usuario
    }

    fun actualizarToken(correo: String, token: String): Boolean {
        return usuarioRepository.updateToken(correo, token)
    }
}