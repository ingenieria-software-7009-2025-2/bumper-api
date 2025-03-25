package com.bumper.api.user.service

import com.bumper.api.user.domain.Incidente
import com.bumper.api.user.repository.IncidenteRepository
import com.bumper.api.user.repository.UsuarioRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class IncidenteService(
    private val incidenteRepository: IncidenteRepository,
    private val usuarioRepository: UsuarioRepository
) {

    // Crear un nuevo incidente
    fun crearIncidente(incidente: Incidente): Incidente {
        require(incidente.usuario.id != null) { "El ID del usuario no puede ser nulo" }
        require(incidente.tipoIncidente.isNotBlank()) { "El tipo de incidente no puede estar vacío" }
        require(incidente.ubicacion.isNotBlank()) { "La ubicación no puede estar vacía" }
        require(incidente.tipoVialidad.isNotBlank()) { "El tipo de vialidad no puede estar vacío" }

        val usuario = usuarioRepository.findById(incidente.usuario.id)
            ?: throw IllegalArgumentException("Usuario no encontrado con ID: ${incidente.usuario.id}")

        val incidenteConHora = incidente.copy(horaIncidente = LocalDateTime.now())
        val savedIncidente = incidenteRepository.save(incidenteConHora)

        // Actualizar el contador de incidentes del usuario
        usuario.numeroIncidentes += 1
        usuarioRepository.save(usuario)

        return savedIncidente
    }

    // Obtener todos los incidentes
    fun obtenerTodosLosIncidentes(): List<Incidente> {
        return incidenteRepository.findAll()
    }

    // Obtener incidentes por usuario
    fun obtenerIncidentesPorUsuario(usuarioId: Long): List<Incidente> {
        val usuario = usuarioRepository.findById(usuarioId)
            ?: throw IllegalArgumentException("Usuario no encontrado con ID: $usuarioId")
        return incidenteRepository.findByUsuarioId(usuarioId)
    }
}