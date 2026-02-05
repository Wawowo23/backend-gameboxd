# Gameboxd
### Loguea cada píxel, reseña cada jefe.

---

## 🛠️ API EndpointsServidor de producción: https://backend-gameboxd-1.onrender.com
### 🎮 VideojuegosMétodoEndpointAcción
| Método     | Endpoint              | Acción                       |
| :--------- | :-------------------- | :--------------------------- |
| **GET**    | `/api/v1/juegos`      | Listar y filtrar el catálogo |
| **POST**   | `/api/v1/juegos`      | Crear un nuevo videojuego    |
| **GET**    | `/api/v1/juegos/{id}` | Ver detalle de un juego      |
| **PUT**    | `/api/v1/juegos/{id}` | Actualizar información       |
| **DELETE** | `/api/v1/juegos/{id}` | Eliminar del sistema         |

### 📁 ColeccionesMétodoEndpointAcción
| Método     | Endpoint                   | Acción                        |
| :--------- | :------------------------- | :---------------------------- |
| **GET**    | `/api/v1/colecciones`      | Listar todas las colecciones  |
| **POST**   | `/api/v1/colecciones`      | Crear nueva colección         |
| **GET**    | `/api/v1/colecciones/{id}` | Ver contenido de la colección |
| **PUT**    | `/api/v1/colecciones/{id}` | Editar metadatos              |
| **DELETE** | `/api/v1/colecciones/{id}` | Borrar colección              |

### 👤 Usuarios & AuthMétodoEndpointAcción
| Método     | Endpoint                | Acción                |
| :--------- | :---------------------- | :-------------------- |
| **POST**   | `/api/v1/usuarios`      | Login de usuario      |
| **POST**   | `/api/v1/usuarios/new`  | Registro de cuenta    |
| **GET**    | `/api/v1/usuarios`      | Listado de perfiles   |
| **GET**    | `/api/v1/usuarios/{id}` | Ver perfil específico |
| **PUT**    | `/api/v1/usuarios/{id}` | Actualizar datos      |
| **DELETE** | `/api/v1/usuarios/{id}` | Eliminar cuenta       |

### ⚖️ ModeraciónMétodoEndpointAcción
| Método     | Endpoint                         | Acción                      |
| :--------- | :------------------------------- | :-------------------------- |
| **GET**    | `/api/v1/juegos_pendientes`      | Listar propuestas           |
| **POST**   | `/api/v1/juegos_pendientes`      | Sugerir nuevo juego         |
| **GET**    | `/api/v1/juegos_pendientes/{id}` | Detalle de la propuesta     |
| **PUT**    | `/api/v1/juegos_pendientes/{id}` | Editar propuesta            |
| **DELETE** | `/api/v1/juegos_pendientes/{id}` | Rechazar o borrar propuesta |

### ✍️ Reseñas & ComunidadMétodoEndpointAcción
| Método    | Endpoint                    | Acción                 |
| :-------- | :-------------------------- | :--------------------- |
| **GET**   | `/api/v1/reviews`           | Ver muro de reseñas    |
| **POST**  | `/api/v1/reviews`           | Publicar nueva crítica |
| **PATCH** | `/api/v1/reviews/like/{id}` | Gestionar “Me gusta”   |

### 🏢 EmpresasMétodoEndpointAcción
| Método   | Endpoint                | Acción                 |
| :------- | :---------------------- | :--------------------- |
| **GET**  | `/api/v1/empresas`      | Directorio de empresas |
| **POST** | `/api/v1/empresas`      | Añadir nueva entidad   |
| **GET**  | `/api/v1/empresas/{id}` | Ver detalle de empresa |
| **PUT**  | `/api/v1/empresas/{id}` | Editar información     |


Nota: Todos los endpoints de escritura (POST, PUT, DELETE) requieren el paso previo de autenticación mediante el token JWT generado en el login.

---

## 🚀 Guía de Inicio Rápido (Local)

Sigue estos pasos para tener el entorno de desarrollo listo en tu máquina.

### 1. Clonar y Preparar
Descarga el código fuente del repositorio y asegúrate de tener tu IDE favorito listo (**IntelliJ**, **VS Code**, etc.).

### 2. Gestión de Dependencias
Añade las siguientes dependencias a tu archivo `pom.xml`. Estas son las piezas clave que hacen que el motor funcione:

| Dependencia | Propósito |
| :--- | :--- |
| **Spring Boot Starter Web** | Convierte la aplicación en un backend funcional. |
| **DevTools** | Recarga automática durante el desarrollo (opcional). |
| **Firebase Admin** | Conexión directa con la base de datos Firestore. |
| **Spring Security** | Capa de seguridad y encriptación de contraseñas. |
| **Spring Starter Cache** | Implementación de caché para mejorar la velocidad. |
| **JJWT (Api, Impl, Jackson)** | Gestión de tokens de sesión seguros. |

---

### 3. Configuración de Firebase
Para conectar el backend con la base de datos, necesitas las credenciales de tu proyecto:

1. Ve a la consola de **Firebase** y crea un proyecto.
2. Crea una base de datos **Cloud Firestore**.
3. Ve a **Configuración del proyecto** > **Cuentas de servicio**.
4. Pulsa en **Generar nueva clave privada** para descargar el archivo `.json`.
5. Mueve ese archivo a la carpeta `src/main/resources`.

#### Integración en el Código
En la clase `FirebaseConfig`, tienes dos opciones para detectar el archivo:

* **Opción A (Recomendada):**
    ```java
    String firebaseJson = new String(Files.readAllBytes(Paths.get(new ClassPathResource("tu-archivo.json").getURI())));
    ```
* **Opción B (Ruta absoluta):**
    ```java
    FileInputStream serviceAccount = new FileInputStream("ruta/a/tu/archivo-firebase.json");
    ```

---

### 4. Variables de Entorno
Crea o modifica el archivo `src/main/resources/application.properties` con estos datos:

```properties
# Configuración del Servidor
spring.application.name=gameboxd-backend
server.port=8080

# Seguridad
jwt.secret=tu_cadena_secreta_muy_larga_y_segura_aqui
