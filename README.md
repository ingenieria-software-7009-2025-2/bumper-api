# Bumper - Reporte Ciudadano de Incidentes Urbanos

## Propósito

Bumper es una aplicación web diseñada para facilitar el reporte y seguimiento de incidentes urbanos por parte de los ciudadanos. Permite a los usuarios informar sobre problemas como baches, iluminación deficiente, acumulación de basura, entre otros, contribuyendo a la mejora de su entorno y la eficiencia de los servicios municipales.

## Casos de Uso

*   **Registro de Usuario:** Un nuevo usuario se registra para reportar incidentes.
*   **Inicio de Sesión:** Un usuario registrado accede a la aplicación.
*   **Reporte de Incidente:** Un usuario autenticado crea un nuevo reporte con ubicación, tipo, descripción y fotos.
*   **Gestión de Reportes:** Un usuario consulta, actualiza el estado o elimina sus reportes.
*   **Visualización de Incidentes:** Usuarios (autenticados o invitados) visualizan incidentes en un mapa.
*   **Cierre de Sesión:** Un usuario autenticado cierra su sesión.
*   **Actualización de Contraseña:** Un usuario autenticado cambia su contraseña.
*   **Descarga de Reporte:** Un usuario descarga una imagen con los detalles de un incidente para compartir en redes sociales.

## API Endpoints

### Usuarios

*   `POST /v1/users/create`: Registra un nuevo usuario.
*   `GET /v1/users/correo/{correo}`: Obtiene un usuario por su correo electrónico.
*   `GET /v1/users/{id}`: Obtiene un usuario por su ID.
*   `POST /v1/users/login`: Inicia sesión de usuario.
*   `POST /v1/users/logout`: Cierra sesión de usuario.
*   `PUT /v1/users`: Actualiza los datos de un usuario.
*   `PUT /v1/users/update-password`: Actualiza la contraseña de un usuario.

### Incidentes

*   `POST /v1/incidentes/create`: Crea un nuevo incidente.
*   `GET /v1/incidentes/all`: Obtiene todos los incidentes.
*   `GET /v1/incidentes/usuario/{usuarioId}`: Obtiene incidentes por usuario.
*   `GET /v1/incidentes/{id}`: Obtiene un incidente por su ID.
*   `PUT /v1/incidentes/update-status/{id}`: Actualiza el estado de un incidente.
*   `GET /v1/incidentes/cercanos`: Busca incidentes cercanos a una ubicación.
*   `DELETE /v1/incidentes/{id}`: Elimina un incidente.

## Tecnología Usada

*   **Backend:**
    *   Kotlin con Spring Boot
*   **Frontend:**
    *   React
*   **Base de Datos:**
    *   PostgreSQL en Supabase
*   **API para Mapas:**
    *   Leaflet con OpenStreetMap
