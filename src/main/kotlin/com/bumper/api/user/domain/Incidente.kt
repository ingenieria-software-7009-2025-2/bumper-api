package com.bumper.api.user.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Table(name = "incidentes")
data class Incidente(
    val id: String? = null,
    val usuarioId: Long,
    val tipoIncidente: String,
    val ubicacion: String,
    val latitud: Double,
    val longitud: Double,
    val horaIncidente: LocalDateTime = LocalDateTime.now(),
    val tipoVialidad: String,
    val estado: String = "PENDIENTE",

    @Column(columnDefinition = "text[]")
    val fotos: List<String> = emptyList()  // Ahora es una lista de URLs
) {
    fun copyWithFotos(newFotos: List<String>): Incidente {
        return this.copy(fotos = newFotos)
    }
}