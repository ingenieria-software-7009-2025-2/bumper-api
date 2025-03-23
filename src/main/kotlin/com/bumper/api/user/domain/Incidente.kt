package com.bumper.api.user.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "incidentes")
data class Incidente(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    val usuario: Usuario,

    @Column(name = "tipo_incidente", nullable = false)
    val tipoIncidente: String,

    @Column(nullable = false)
    val ubicacion: String,

    @Column(name = "hora_incidente", nullable = false)
    val horaIncidente: LocalDateTime,

    @Column(name = "tipo_vialidad", nullable = false)
    val tipoVialidad: String
)