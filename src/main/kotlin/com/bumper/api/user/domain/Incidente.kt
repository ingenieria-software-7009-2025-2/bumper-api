package com.bumper.api.user.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "incidentes")
data class Incidente(
    @Id
    @Column(nullable = false)
    val id: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    val usuario: Usuario,

    @Column(name = "tipo_incidente", nullable = false)
    val tipoIncidente: String,

    @Column(nullable = false)
    val ubicacion: String,

    @Column(nullable = false)
    val latitud: Double,

    @Column(nullable = false)
    val longitud: Double,

    @Column(name = "hora_incidente", nullable = false)
    val horaIncidente: LocalDateTime = LocalDateTime.now(),

    @Column(name = "tipo_vialidad", nullable = false)
    val tipoVialidad: String,

    @Column(nullable = false)
    val estado: String = "PENDIENTE",

    @OneToMany(mappedBy = "incidenteId", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val fotos: List<FotoIncidente> = emptyList()
) {
    // Método para facilitar la copia con fotos actualizadas
    fun copyWithFotos(newFotos: List<FotoIncidente>): Incidente {
        return this.copy(fotos = newFotos)
    }
}