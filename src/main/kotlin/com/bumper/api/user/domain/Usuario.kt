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

    @Column(nullable = false, length = 100)
    val nombre: String,

    @Column(nullable = false, length = 100)
    val apellido: String,

    @Column(unique = true, nullable = false, length = 255)
    val correo: String,

    @Column(nullable = false, length = 255)
    val password: String,

    @Column(length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'inactivo'")
    var token: String = "inactivo",

    @Column(name = "numero_incidentes", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    var numeroIncidentes: Int = 0,

    @Column(name = "fecha_registro", nullable = false, updatable = false,
        columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    val fechaRegistro: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "usuario", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val incidentes: List<Incidente> = emptyList()
) {
    // Método para validar el correo electrónico
    fun isValidEmail(): Boolean {
        return correo.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)$"))
    }

    // Método para crear una copia con número de incidentes actualizado
    fun incrementarIncidentes(): Usuario {
        return this.copy(numeroIncidentes = this.numeroIncidentes + 1)
    }

    // Método para actualizar el token
    fun actualizarToken(nuevoToken: String): Usuario {
        return this.copy(token = nuevoToken)
    }

    companion object {
        const val MAX_NOMBRE_LENGTH = 100
        const val MAX_APELLIDO_LENGTH = 100
        const val MAX_CORREO_LENGTH = 255
        const val MAX_PASSWORD_LENGTH = 255
        const val MAX_TOKEN_LENGTH = 20
    }
}