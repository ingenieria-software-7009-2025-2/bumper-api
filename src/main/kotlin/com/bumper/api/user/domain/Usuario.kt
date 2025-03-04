package com.bumper.bumper.api

data class Usuario(
    @field:NotNull
    @field:Email
    val mail: String,

    @field:NotNull
    @field:Size(min = 2, max = 50)
    val nombre: String,

    @field:NotNull
    @field:Size(min = 2, max = 50)
    val apellido: String,

    @field:NotNull
    @field:Size(min = 6, max = 20)
    val password: String,

    var token: String = "inactivo"
)