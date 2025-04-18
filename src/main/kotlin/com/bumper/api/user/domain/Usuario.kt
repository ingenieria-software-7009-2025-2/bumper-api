package com.bumper.api.user.domain

import jakarta.persistence.*
import java.time.LocalDateTime
import com.bumper.api.user.domain.Incidente

@Entity
@Table(name = "usuarios")
data class Usuario(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val nombre: String,

    @Column(nullable = false)
    val apellido: String,

    @Column(unique = true, nullable = false)
    val correo: String,

    @Column(nullable = false)
    val password: String,

    @Column(columnDefinition = "VARCHAR(20) DEFAULT 'inactivo'")
    var token: String = "inactivo",

    @Column(name = "numero_incidentes", nullable = false)
    var numeroIncidentes: Int = 0,

    @Column(name = "fecha_registro", nullable = false)
    val fechaRegistro: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "usuario", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val incidentes: List<Incidente> = listOf()
)