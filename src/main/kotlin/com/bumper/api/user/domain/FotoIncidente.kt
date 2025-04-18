package com.bumper.api.user.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "fotos_incidentes")
data class FotoIncidente(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incidente_id", nullable = false)
    val incidente: Incidente,

    @Column(name = "url_foto", nullable = false)
    val urlFoto: String,

    @Column
    val descripcion: String? = null,

    @Column(name = "fecha_subida", nullable = false)
    val fechaSubida: LocalDateTime = LocalDateTime.now()
)