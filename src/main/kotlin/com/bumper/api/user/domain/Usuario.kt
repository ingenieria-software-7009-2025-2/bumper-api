package com.bumper.api.user.domain

import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Id
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Column

@Entity
@Table(name = "usuarios")
data class Usuario(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 🚀 Usa IDENTITY para PostgreSQL
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val mail: String,

    @Column(nullable = false)
    val nombre: String,

    @Column(nullable = false)
    val apellido: String,

    @Column(nullable = false)
    val password: String,

    @Column(columnDefinition = "VARCHAR(255) DEFAULT 'inactivo'")
    var token: String = "inactivo"
)