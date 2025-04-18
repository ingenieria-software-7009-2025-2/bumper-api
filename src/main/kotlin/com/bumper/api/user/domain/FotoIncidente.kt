package com.bumper.api.user.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "fotos_incidentes")
data class FotoIncidente(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "incidente_id", nullable = false)
    val incidenteId: String,

    @Column(name = "url_foto", nullable = false)
    val urlFoto: String,

    @Column
    val descripcion: String? = null,

    @Column(name = "fecha_subida", nullable = false)
    val fechaSubida: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FotoIncidente

        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "FotoIncidente(id=$id, incidenteId='$incidenteId', urlFoto='$urlFoto')"
    }
}