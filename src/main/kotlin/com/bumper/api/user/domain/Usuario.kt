// Usuario.kt
package com.bumper.api.user.domain

import java.time.LocalDateTime

data class Usuario(
    val id: Long? = null,
    val nombre: String,
    val apellido: String,
    val correo: String,
    val password: String,
    val token: String = TOKEN_INACTIVO,
    val numeroIncidentes: Int = 0,
    val fechaRegistro: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        const val TOKEN_ACTIVO = "activo"
        const val TOKEN_INACTIVO = "inactivo"
    }
}