package com.bumper.api.user.domain

data class Usuario(
    val id: Long = 0,
    val mail: String,
    val nombre: String,
    val apellido: String,
    val password: String,
    var token: String = "inactivo"
)