package com.bumper.api.user.service

import com.bumper.api.user.domain.Usuario
import com.bumper.api.user.repository.UsuarioRepository
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory

@Service
class UsuarioService(private val usuarioRepository: UsuarioRepository) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun registrarUsuario(usuario: Usuario): Usuario {
        logger.info("Registrando nuevo usuario: ${usuario.correo}")
        return usuarioRepository.save(usuario)
    }

    fun buscarPorCorreo(correo: String): Usuario? {
        return usuarioRepository.findByCorreo(correo)
    }

    fun actualizarUsuario(usuario: Usuario): Usuario {
        logger.info("Actualizando usuario: ${usuario.id}")
        return usuarioRepository.save(usuario)
    }

    fun actualizarToken(correo: String, nuevoToken: String): Boolean {
        logger.info("Actualizando token para usuario: $correo")
        return usuarioRepository.updateToken(correo, nuevoToken)
    }

    fun validarCredenciales(correo: String, password: String): Usuario {
        val usuario = usuarioRepository.findByCorreo(correo)
            ?: throw IllegalArgumentException("Usuario no encontrado")

        if (usuario.password != password) {
            throw IllegalArgumentException("Contraseña incorrecta")
        }

        // Activar el token y guardar
        return usuario.copy(token = Usuario.TOKEN_ACTIVO).let {
            usuarioRepository.updateToken(correo, Usuario.TOKEN_ACTIVO)
            usuarioRepository.findByCorreo(correo)
                ?: throw IllegalStateException("Error al actualizar el token")
        }
    }

    fun cerrarSesion(correo: String): Boolean {
        return actualizarToken(correo, Usuario.TOKEN_INACTIVO)
    }

    fun buscarPorId(id: Long): Usuario? {
        return usuarioRepository.findById(id)
    }

    fun buscarPorIds(ids: List<Long>): List<Usuario> {
        return usuarioRepository.findByIds(ids)
    }
}