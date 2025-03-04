import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import javax.sql.DataSource

@Repository
class UsuarioRepository(private val dataSource: DataSource) {

    private val jdbcTemplate = JdbcTemplate(dataSource)

    // Mapeador de filas para convertir ResultSet en un objeto Usuario
    private val usuarioRowMapper = RowMapper { rs, _ ->
        Usuario(
            id = rs.getLong("id"),
            nombre = rs.getString("nombre"),
            apellido = rs.getString("apellido"),
            mail = rs.getString("mail"),
            password = rs.getString("password"),
            token = rs.getString("token")
        )
    }

    // Guardar un usuario en la base de datos
    fun save(usuarioData: Usuario): Usuario {
        val sql = """
            INSERT INTO usuarios (nombre, apellido, mail, password, token)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id
        """ 
        val id = jdbcTemplate.queryForObject(
            sql,
            arrayOf(
                usuarioData.nombre,
                usuarioData.apellido,
                usuarioData.mail,
                usuarioData.password,
                usuarioData.token
            ),
            Long::class.java
        )
        return usuarioData.copy(id = id ?: throw IllegalStateException("No se pudo guardar el usuario"))
    }

    // Buscar un usuario por su correo electrónico
    fun findByMail(mail: String): Usuario? {
        val sql = "SELECT * FROM usuarios WHERE mail = ?"
        return jdbcTemplate.query(sql, usuarioRowMapper, mail).firstOrNull()
    }

    // Actualizar el token de un usuario
    fun updateToken(mail: String, token: String) {
        val sql = "UPDATE usuarios SET token = ? WHERE mail = ?"
        jdbcTemplate.update(sql, token, mail)
    }
}