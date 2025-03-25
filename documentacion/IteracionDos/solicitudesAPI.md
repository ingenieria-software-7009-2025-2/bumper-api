# Documentación de la API de Usuarios

Esta documentación describe los endpoints para la gestión de usuarios en la API.

## 1. Crear un nuevo usuario

**Endpoint:** `POST /v1/users/create`

**Descripción:** Crea un nuevo usuario en la base de datos.

**Solicitud:**

* **URL:** `http://localhost:8080/v1/users/create`
* **Método:** `POST`
* **Headers:** `Content-Type: application/json`
* **Body (raw, JSON):**

    ```json
    {
        "nombre": "Juan",
        "apellido": "Pérez",
        "correo": "juan@example.com",
        "password": "1234",
        "token": "inactivo",
        "numeroIncidentes": 0
    }
    ```

**Respuesta esperada (éxito, HTTP 201):**

    ```json
    {
        "id": 1,
        "nombre": "Juan",
        "apellido": "Pérez",
        "correo": "juan@example.com",
        "password": "1234",
        "token": "inactivo",
        "numeroIncidentes": 0,
        "incidentes": []
    }
    ```

**Notas:**

* El `id` será generado automáticamente por la base de datos.

* `token` y `numeroIncidentes` tienen valores por defecto, pero los incluimos para ser explícitos.


## 2. Iniciar sesión (Login)

**Endpoint:** `POST /v1/users/login`

**Descripción:** Inicia sesión con el usuario creado y cambia el token a `"activo"`.

**Solicitud:**

* **URL:** `http://localhost:8080/v1/users/login`
* **Método:** `POST`
* **Headers:** `Content-Type: application/json`
* **Body (raw, JSON):**

    ```json
    {
        "correo": "juan@example.com",
        "password": "1234"
    }
    ```

**Respuesta esperada (éxito, HTTP 200):**

    ```json
    {
        "id": 1,
        "nombre": "Juan",
        "apellido": "Pérez",
        "correo": "juan@example.com",
        "password": "1234",
        "token": "activo",
        "numeroIncidentes": 0,
        "incidentes": []
    }
    ```

**Notas:**

* Verifica que el token cambie a `activo`.

* Si las credenciales son incorrectas, recibirás un HTTP `401` con un mensaje como `Credenciales inválidas`


## 3. Crear un nuevo incidente

**Endpoint:** `POST /v1/incidentes`

**Descripción:** Crea un incidente asociado al usuario creado.

**Solicitud:**

* **URL:** `http://localhost:8080/v1/incidentes`
* **Método:** `POST`
* **Headers:** `Content-Type: application/json`
* **Body (raw, JSON):**

    ```json
    {
        "usuarioId": 1,
        "tipoIncidente": "BACHES",
        "ubicacion": "Av. Principal 123",
        "tipoVialidad": "AVENIDA"
    }
    ```

**Respuesta esperada (éxito, HTTP 201):**

    ```json
    {
        "id": 1,
        "usuario": {
            "id": 1,
            "nombre": "Juan",
            "apellido": "Pérez",
            "correo": "juan@example.com",
            "password": "1234",
            "token": "activo",
            "numeroIncidentes": 1,
            "incidentes": []
        },
        "tipoIncidente": "BACHES",
        "ubicacion": "Av. Principal 123",
        "horaIncidente": "2025-03-23T10:00:00",
        "tipoVialidad": "AVENIDA"
    }
    ```
**Notas:**

* El `usuarioId` debe coincidir con el `id` del usuario creado (en este caso, `1`).

* `horaIncidente` será asignada automáticamente por el servidor (por ejemplo, la hora actual).

* Verifica que `numeroIncidentes` del usuario se incremente a `1`.


## 4. Recuperar todos los incidentes

**Endpoint:** `GET /v1/incidentes`

**Descripción:** Lista todos los incidentes en la base de datos.

**Solicitud:**

* **URL:** `http://localhost:8080/v1/incidentes`
* **Método:** `GET`
* **Headers:** *(ninguno requerido)*
* **Body:** *(ninguno)*

**Respuesta esperada (éxito, HTTP 200):**

    ```json
    [
        {
            "id": 1,
            "usuario": {
                "id": 1,
                "nombre": "Juan",
                "apellido": "Pérez",
                "correo": "juan@example.com",
                "password": "1234",
                "token": "activo",
                "numeroIncidentes": 1,
                "incidentes": []
            },
            "tipoIncidente": "BACHES",
            "ubicacion": "Av. Principal 123",
            "horaIncidente": "2025-03-23T10:00:00",
            "tipoVialidad": "AVENIDA"
        }
    ]
    ```

**Notas:**

* Se debe visualizar el incidente que se ha creado.


## 5. Recuperar incidentes por usuario

**Endpoint:** `GET /v1/incidentes/usuario/{usuarioId}`

**Descripción:** Lista los incidentes asociados a un usuario específico.

**Solicitud:**

* **URL:** `http://localhost:8080/v1/incidentes/usuario/1`
* **Método:** `GET`
* **Headers:** *(ninguno requerido)*
* **Body:** *(ninguno)*

**Respuesta esperada (éxito, HTTP 200):**

    ```json
    [
        {
            "id": 1,
            "usuario": {
                "id": 1,
                "nombre": "Juan",
                "apellido": "Pérez",
                "correo": "juan@example.com",
                "password": "1234",
                "token": "activo",
                "numeroIncidentes": 1,
                "incidentes": []
            },
            "tipoIncidente": "BACHES",
            "ubicacion": "Av. Principal 123",
            "horaIncidente": "2025-03-23T10:00:00",
            "tipoVialidad": "AVENIDA"
        }
    ]
    ```

**Notas:**

* Cambia el `{usuarioId}` (`1` en este caso) según el ID del usuario creado.

## 6. Obtener información del usuario (verificación)

**Endpoint:** `GET /v1/users/me`

**Descripción:** Obtiene los detalles del usuario, incluyendo el contador de incidentes actualizado.

**Solicitud:**

* **URL:** `http://localhost:8080/v1/users/me`
* **Método:** `GET`
* **Headers:**
    * `correo: juan@example.com`
* **Body:** *(ninguno)*

**Respuesta esperada (éxito, HTTP 200):**

    ```json
    {
        "id": 1,
        "nombre": "Juan",
        "apellido": "Pérez",
        "correo": "juan@example.com",
        "password": "1234",
        "token": "activo",
        "numeroIncidentes": 1,
        "incidentes": []
    }
    ```

**Notas:**

* El header `correo` debe coincidir con el correo del usuario.
* Verifica que `numeroIncidentes` sea `1` después de crear el incidente.  

## 7. Cerrar sesión (Logout)

**Endpoint:** `POST /v1/users/logout`

**Descripción:** Cierra la sesión del usuario, cambiando el token a `"inactivo"`.

**Solicitud:**

* **URL:** `http://localhost:8080/v1/users/logout`
* **Método:** `POST`
* **Headers:**
    * `correo: juan@example.com`
* **Body:** *(ninguno)*

**Respuesta esperada (éxito, HTTP 200):**

    ```json

    "Sesión cerrada correctamente"

    ```

**Notas:**

* Usa el mismo header `correo` que en el paso anterior.
* Para verificar, haz otra solicitud a `GET /v1/users/me` y el `token` debería ser `"inactivo"`.  
