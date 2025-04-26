package com.bumper.api.user.domain

import java.time.LocalDateTime

data class FotoIncidente(
    val id: Long? = null,
    val incidenteId: String,
    val urlFoto: String,
    val descripcion: String? = null,
    val fechaSubida: LocalDateTime = LocalDateTime.now()
) {
    override fun toString(): String {
        return "FotoIncidente(id=$id, incidenteId='$incidenteId', urlFoto='$urlFoto')"
    }
}