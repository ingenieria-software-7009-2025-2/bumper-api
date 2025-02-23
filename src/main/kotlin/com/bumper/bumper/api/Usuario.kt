package com.bumper.bumper.api

data class Usuario(
    val mail: String,
    val password: String,
    val token: String? = null
)
