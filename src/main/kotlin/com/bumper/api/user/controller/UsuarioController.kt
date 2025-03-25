package com.bumper.api.user.controller

import com.bumper.api.user.domain.Usuario
import com.bumper.api.user.service.UsuarioService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/users")
class UsuarioController(private val usuarioService: UsuarioService) {

    private val logger: Logger = LoggerFactory.getLogger(UsuarioController::class.java)

    @GetMapping("/hello")
    fun hello(): ResponseEntity<String> {
        return ResponseEntity.ok("¡Hola desde el controlador de usuarios!")
    }

    /**
     * Endpoint para crear un nuevo usuario en el sistema.
     *
     * @param usuarioData Representa los datos del usuario que se desea crear. Esta información se recibe
     *                    en el cuerpo de la solicitud HTTP gracias a la anotación `@RequestBody`.
     *                    Se espera que el objeto [Usuario] contenga todos los campos necesarios
     *                    para la creación de un usuario (por ejemplo, nombre, correo, contraseña, etc.).
     *
     * @return Retorna una respuesta HTTP encapsulada en un objeto [ResponseEntity]:
     *         - Si el usuario se crea exitosamente, retorna un estado HTTP 201 (CREATED) junto con el
     *           objeto [Usuario] creado en el cuerpo de la respuesta.
     *         - Si ocurre un error durante la creación del usuario, retorna un estado HTTP 400 (BAD REQUEST)
     *           sin cuerpo en la respuesta.
     *
     * @throws Exception En caso de que ocurra un error durante el proceso de creación del usuario,
     *                   se captura la excepción y se registra un mensaje de error en el logger.
     *                   Esto permite manejar errores de manera controlada y evitar que la aplicación
     *                   falle abruptamente.
     */
    @PostMapping("/create") // url para crear usuario
    fun createUser(@RequestBody usuarioData: Usuario): ResponseEntity<Usuario> {
        return try {
            // Registra en el logger la intención de crear un usuario, incluyendo su correo para identificación.
            logger.info("Creando usuario con correo: ${usuarioData.correo}")

            // Llama al servicio `usuarioService` para crear el usuario en la base de datos o sistema subyacente.
            val nuevoUsuario = usuarioService.crearUsuario(usuarioData)

            // Retorna una respuesta HTTP con estado 201 (CREATED) y el usuario creado en el cuerpo.
            ResponseEntity.status(HttpStatus.CREATED).body(nuevoUsuario)
        } catch (e: Exception) {
            // Registra en el logger el error ocurrido, incluyendo el mensaje de la excepción y el stack trace.
            logger.error("Error al crear usuario: ${e.message}", e)

            // Retorna una respuesta HTTP con estado 400 (BAD REQUEST) en caso de error.
            ResponseEntity.badRequest().build()
        }
    }


    /**
     * Endpoint para iniciar sesión en el sistema.
     *
     * @param credenciales Representa un mapa que contiene las credenciales del usuario (correo y contraseña).
     *                     Este mapa se recibe en el cuerpo de la solicitud HTTP gracias a la anotación `@RequestBody`.
     *                     Se espera que el mapa contenga las claves "correo" y "password".
     *
     * @return Retorna una respuesta HTTP encapsulada en un objeto [ResponseEntity]:
     *         - Si el inicio de sesión es exitoso, retorna un estado HTTP 200 (OK) junto con los datos del usuario
     *           autenticado en el cuerpo de la respuesta.
     *         - Si las credenciales son inválidas, retorna un estado HTTP 401 (UNAUTHORIZED) con un mensaje
     *           indicando que las credenciales son incorrectas.
     *         - Si faltan el correo o la contraseña, retorna un estado HTTP 400 (BAD REQUEST) con un mensaje
     *           indicando que ambos campos son requeridos.
     *
     * @throws IllegalArgumentException Si las credenciales proporcionadas son inválidas, el servicio lanzará
     *                                  una excepción de tipo [IllegalArgumentException], que será capturada
     *                                  y manejada adecuadamente.
     */
    @PostMapping("/login") // url para hacer login
    fun iniciarSesion(@RequestBody credenciales: Map<String, String>): ResponseEntity<Any> {
        // Extrae el correo y la contraseña del mapa de credenciales.
        val correo = credenciales["correo"]
        val password = credenciales["password"]

        // Registra en el logger el intento de inicio de sesión con el correo proporcionado.
        logger.info("Intentando iniciar sesión con correo: $correo")

        // Verifica si el correo o la contraseña están vacíos o nulos.
        if (correo.isNullOrBlank() || password.isNullOrBlank()) {
            // Registra una advertencia en el logger si alguno de los campos está vacío.
            logger.warn("Correo o contraseña vacíos")
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Correo y contraseña son requeridos")
        }

        return try {
            // Llama al servicio `usuarioService` para verificar las credenciales y autenticar al usuario.
            val usuario = usuarioService.iniciarSesion(correo, password)

            // Registra en el logger el éxito del inicio de sesión.
            logger.info("Inicio de sesión exitoso para el usuario con correo: $correo")

            // Retorna una respuesta HTTP con estado 200 (OK) y los datos del usuario autenticado.
            ResponseEntity.ok(usuario)
        } catch (e: IllegalArgumentException) {
            // Registra en el logger el error ocurrido durante el inicio de sesión.
            logger.error("Error en inicio de sesión: ${e.message}", e)

            // Retorna una respuesta HTTP con estado 401 (UNAUTHORIZED) y un mensaje de error.
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales inválidas")
        }
    }


    /**
     * Endpoint para cerrar sesión en el sistema.
     *
     * @param correo Representa el correo del usuario que desea cerrar sesión. Este valor se recibe
     *               mediante un encabezado HTTP con el nombre "correo", gracias a la anotación `@RequestHeader`.
     *
     * @return Retorna una respuesta HTTP encapsulada en un objeto [ResponseEntity]:
     *         - Si el cierre de sesión es exitoso, retorna un estado HTTP 200 (OK) junto con un mensaje
     *           indicando que la sesión ha sido cerrada correctamente.
     *         - Si el usuario no se encuentra o no puede cerrar sesión, retorna un estado HTTP 404 (NOT FOUND)
     *           con un mensaje indicando que el usuario no fue encontrado.
     *
     * @throws IllegalArgumentException Si el usuario no existe o no puede cerrar sesión, el servicio lanzará
     *                                  una excepción de tipo [IllegalArgumentException], que será capturada
     *                                  y manejada adecuadamente.
     */
    @PostMapping("/logout")
    fun cerrarSesion(@RequestHeader("correo") correo: String): ResponseEntity<String> {
        // Registra en el logger el intento de cierre de sesión para el usuario con el correo proporcionado.
        logger.info("Cerrando sesión para el usuario con correo: $correo")

        return try {
            // Llama al servicio `usuarioService` para cerrar la sesión del usuario.
            usuarioService.cerrarSesion(correo)

            // Registra en el logger el éxito del cierre de sesión.
            logger.info("Sesión cerrada correctamente para el usuario con correo: $correo")

            // Retorna una respuesta HTTP con estado 200 (OK) y un mensaje de éxito.
            ResponseEntity.ok("Sesión cerrada correctamente")
        } catch (e: IllegalArgumentException) {
            // Registra en el logger el error ocurrido durante el cierre de sesión.
            logger.error("Error al cerrar sesión: ${e.message}", e)

            // Retorna una respuesta HTTP con estado 404 (NOT FOUND) y un mensaje indicando que el usuario no fue encontrado.
            ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado")
        }
    }


    /**
     * Endpoint para obtener la información de un usuario autenticado.
     *
     * @param correo Representa el correo del usuario cuya información se desea obtener. Este valor se recibe
     *               mediante un encabezado HTTP con el nombre "correo", gracias a la anotación `@RequestHeader`.
     *
     * @return Retorna una respuesta HTTP encapsulada en un objeto [ResponseEntity]:
     *         - Si el usuario es encontrado, retorna un estado HTTP 200 (OK) junto con los datos del usuario
     *           en el cuerpo de la respuesta.
     *         - Si el usuario no es encontrado, retorna un estado HTTP 404 (NOT FOUND) con un mensaje
     *           indicando que el usuario no fue encontrado.
     */
    @GetMapping("/me")
    fun obtenerUsuario(@RequestHeader("correo") correo: String): ResponseEntity<Any> {
        // Registra en el logger el intento de obtener la información del usuario con el correo proporcionado.
        logger.info("Obteniendo información para el usuario con correo: $correo")

        // Llama al servicio `usuarioService` para buscar al usuario por su correo.
        val usuario = usuarioService.obtenerUsuario(correo)

        // Verifica si el usuario fue encontrado.
        return if (usuario != null) {
            // Registra en el logger los detalles del usuario encontrado, incluyendo su correo y número de incidentes.
            logger.info("Usuario encontrado: ${usuario.correo}, incidentes: ${usuario.numeroIncidentes}")

            // Retorna una respuesta HTTP con estado 200 (OK) y los datos del usuario en el cuerpo.
            ResponseEntity.ok(usuario)
        } else {
            // Registra en el logger una advertencia si el usuario no fue encontrado.
            logger.warn("Usuario no encontrado con correo: $correo")

            // Retorna una respuesta HTTP con estado 404 (NOT FOUND) y un mensaje indicando que el usuario no existe.
            ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado")
        }
    }


    /**
     * Endpoint para actualizar la información de un usuario existente.
     *
     * @param correo Representa el correo del usuario cuya información se desea actualizar. Este valor se recibe
     *               mediante un encabezado HTTP con el nombre "correo", gracias a la anotación `@RequestHeader`.
     *
     * @param newData Representa los nuevos datos del usuario que se desean actualizar. Este objeto se recibe
     *                en el cuerpo de la solicitud HTTP gracias a la anotación `@RequestBody`.
     *                Solo se permiten actualizar los campos `nombre`, `apellido` y `password`. Los campos
     *                `correo` y `numeroIncidentes` no son modificables en este endpoint.
     *
     * @return Retorna una respuesta HTTP encapsulada en un objeto [ResponseEntity]:
     *         - Si la actualización es exitosa, retorna un estado HTTP 200 (OK) junto con los datos del usuario
     *           actualizado en el cuerpo de la respuesta.
     *         - Si el usuario no es encontrado, retorna un estado HTTP 404 (NOT FOUND) con un mensaje
     *           indicando que el usuario no fue encontrado.
     *
     * @throws IllegalArgumentException Si el usuario no existe, se lanza una excepción de tipo
     *                                  [IllegalArgumentException], que es capturada y manejada adecuadamente.
     */
    @PutMapping("/update")
    fun actualizarUsuario(
        @RequestHeader("correo") correo: String,
        @RequestBody newData: Usuario
    ): ResponseEntity<Any> {
        // Registra en el logger el intento de actualizar la información del usuario con el correo proporcionado.
        logger.info("Actualizando información para el usuario con correo: $correo")

        return try {
            // Busca al usuario existente en el sistema utilizando su correo.
            val usuarioExistente = usuarioService.obtenerUsuario(correo)
                ?: throw IllegalArgumentException("Usuario no encontrado")

            // Actualiza los datos del usuario existente con los nuevos valores proporcionados.
            // Se excluyen campos como `correo` y `numeroIncidentes`, ya que no deben ser modificados aquí.
            val usuarioActualizado = usuarioService.actualizarUsuario(
                usuarioExistente.copy(
                    nombre = newData.nombre,
                    apellido = newData.apellido,
                    password = newData.password
                )
            )

            // Registra en el logger el éxito de la actualización del usuario.
            logger.info("Usuario actualizado con éxito: ${usuarioActualizado.correo}")

            // Retorna una respuesta HTTP con estado 200 (OK) y los datos del usuario actualizado en el cuerpo.
            ResponseEntity.ok(usuarioActualizado)
        } catch (e: IllegalArgumentException) {
            // Registra en el logger el error ocurrido durante la actualización del usuario.
            logger.error("Error al actualizar usuario: ${e.message}", e)

            // Retorna una respuesta HTTP con estado 404 (NOT FOUND) y un mensaje indicando que el usuario no fue encontrado.
            ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado")
        }
    }
}