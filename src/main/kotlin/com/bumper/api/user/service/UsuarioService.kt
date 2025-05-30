package com.bumper.api.user.service

import com.bumper.api.user.domain.Usuario
import com.bumper.api.user.repository.UsuarioRepository
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory

/**
 * Servicio de negocio para la gestión de usuarios
 * Implementa la lógica de negocio y coordina las operaciones con el repositorio
 * Actúa como capa intermedia entre el controlador y la persistencia de datos
 */
@Service
class UsuarioService(private val usuarioRepository: UsuarioRepository) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Registra un nuevo usuario en el sistema
     * Delega directamente al repositorio para la persistencia
     * @param usuario Entidad Usuario con los datos a persistir
     * @return Usuario guardado con ID generado y datos completos
     */
    fun registrarUsuario(usuario: Usuario): Usuario {
        logger.info("Registrando nuevo usuario: ${usuario.correo}")

        // Delegación directa al repositorio para persistir el usuario
        return usuarioRepository.save(usuario)
    }

    /**
     * Busca un usuario por su dirección de correo electrónico
     * Operación de consulta simple sin validaciones adicionales
     * @param correo Dirección de correo electrónico del usuario
     * @return Usuario encontrado o null si no existe
     */
    fun buscarPorCorreo(correo: String): Usuario? {
        // Búsqueda directa en repositorio sin lógica de negocio adicional
        return usuarioRepository.findByCorreo(correo)
    }

    /**
     * Actualiza todos los datos de un usuario existente
     * Utiliza el método save del repositorio que maneja tanto insert como update
     * @param usuario Entidad Usuario con datos actualizados (debe incluir ID)
     * @return Usuario actualizado con los cambios persistidos
     */
    fun actualizarUsuario(usuario: Usuario): Usuario {
        logger.info("Actualizando usuario: ${usuario.id}")

        // El repositorio determina si es insert o update basado en la presencia del ID
        return usuarioRepository.save(usuario)
    }

    /**
     * Actualiza únicamente el token de sesión de un usuario
     * Operación específica para gestión de estados de autenticación
     * @param correo Identificador del usuario por correo
     * @param nuevoToken Nuevo valor del token (activo/inactivo)
     * @return true si la actualización fue exitosa, false en caso contrario
     */
    fun actualizarToken(correo: String, nuevoToken: String): Boolean {
        logger.info("Actualizando token para usuario: $correo")

        // Actualización específica del campo token mediante query optimizada
        return usuarioRepository.updateToken(correo, nuevoToken)
    }

    /**
     * Valida las credenciales de un usuario y activa su sesión
     * Implementa la lógica de autenticación completa del sistema
     * @param correo Dirección de correo del usuario
     * @param password Contraseña en texto plano para validar
     * @return Usuario autenticado con token activo
     * @throws IllegalArgumentException si las credenciales son inválidas
     */
    fun validarCredenciales(correo: String, password: String): Usuario {
        // Búsqueda del usuario por correo electrónico
        val usuario = usuarioRepository.findByCorreo(correo)
            ?: throw IllegalArgumentException("Usuario no encontrado")

        // Validación de contraseña mediante comparación directa
        if (usuario.password != password) {
            throw IllegalArgumentException("Credenciales no validas, intente de nuevo")
        }

        // Activación del token de sesión y recuperación del usuario actualizado
        return usuario.copy(token = Usuario.TOKEN_ACTIVO).let {
            // Actualización del token en base de datos
            usuarioRepository.updateToken(correo, Usuario.TOKEN_ACTIVO)

            // Recuperación del usuario con token actualizado para retornar
            usuarioRepository.findByCorreo(correo)
                ?: throw IllegalStateException("Error al actualizar el token")
        }
    }

    /**
     * Cierra la sesión de un usuario desactivando su token
     * Operación de logout que invalida la sesión actual
     * @param correo Identificador del usuario por correo
     * @return true si el logout fue exitoso, false en caso contrario
     */
    fun cerrarSesion(correo: String): Boolean {
        // Reutilización del método actualizarToken para establecer estado inactivo
        return actualizarToken(correo, Usuario.TOKEN_INACTIVO)
    }

    /**
     * Busca un usuario por su identificador único numérico
     * Operación de consulta directa por clave primaria
     * @param id Identificador único del usuario
     * @return Usuario encontrado o null si no existe
     */
    fun buscarPorId(id: Long): Usuario? {
        // Búsqueda optimizada por clave primaria
        return usuarioRepository.findById(id)
    }

    /**
     * Busca múltiples usuarios por sus identificadores únicos
     * Operación de consulta batch para optimizar múltiples búsquedas
     * @param ids Lista de identificadores de usuarios a buscar
     * @return Lista de usuarios encontrados (puede ser menor que la lista de IDs)
     */
    fun buscarPorIds(ids: List<Long>): List<Usuario> {
        // Consulta batch optimizada para múltiples IDs
        return usuarioRepository.findByIds(ids)
    }

    /**
     * Actualiza únicamente la contraseña de un usuario específico
     * Operación segura que modifica solo el campo de contraseña
     * Incluye validaciones de existencia del usuario y formato de contraseña
     * @param id Identificador único del usuario
     * @param nuevaPassword Nueva contraseña en texto plano
     * @return true si la actualización fue exitosa, false en caso contrario
     * @throws IllegalArgumentException si el usuario no existe o la contraseña es inválida
     */
    fun actualizarPassword(id: Long, nuevaPassword: String): Boolean {
        logger.info("Actualizando contraseña para usuario ID: $id")

        // Validación de existencia del usuario antes de proceder
        val usuario = usuarioRepository.findById(id)
            ?: throw IllegalArgumentException("Usuario no encontrado")

        // Validación de formato de la nueva contraseña
        if (nuevaPassword.isBlank()) {
            throw IllegalArgumentException("La contraseña no puede estar vacía")
        }

        // Actualización específica del campo contraseña mediante query optimizada
        return usuarioRepository.updatePassword(id, nuevaPassword)
    }
}