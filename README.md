# 🎓 TeachMate

**TeachMate** es una plataforma web desarrollada en **Java y JSP** para coordinar y dar seguimiento a **tutorías entre docentes y alumnos**.

Es una aplicación monolítica: los Servlets procesan las solicitudes, JDBC accede a MySQL y las vistas JSP generan las páginas que utiliza cada rol.

---

## 🚀 Tecnologías utilizadas

- **Java 17** y **Maven**.
- **JSP, Servlets y JSTL** para las vistas y los flujos del servidor.
- **MySQL 8** mediante JDBC.
- **Apache Tomcat 9** como servidor de aplicaciones.
- **HTML5, CSS3, Bootstrap 5 y JavaScript** para la interfaz.
- **Docker** para el despliegue en Render, con MySQL externo.

---

## 📄 Funcionalidades por rol

### 👑 Administrador

- Consultar indicadores de actividad, alumnos registrados y docentes activos.
- Registrar y editar docentes; desactivar o reactivar sus cuentas conservando el historial.
- Agregar especialidades.
- Buscar tutorías por alumno, docente o tema y filtrarlas por estado y fechas.
- Cancelar solicitudes pendientes o aceptadas para resolver incidencias, incluso si ya comenzaron.

### 🧑‍🏫 Docente

- Consultar sus propios indicadores, solicitudes por revisar y próxima sesión confirmada.
- Aceptar o rechazar solicitudes pendientes y cancelar antes del inicio.
- Marcar una tutoría aceptada como completada cuando termine su horario.
- Registrar comentarios sin modificar el estado de la sesión.
- Consultar su historial con filtros y paginación.
- Editar su perfil y cambiar su contraseña.

### 👨‍🎓 Alumno

- Crear una cuenta e iniciar sesión para solicitar tutorías.
- Elegir docente, fecha, hora y duración; conservar los datos del formulario ante un conflicto de horario.
- Consultar su resumen personal, próxima sesión, estados y comentarios.
- Buscar y filtrar sus tutorías, con resultados paginados.
- Cancelar sus propias solicitudes pendientes o aceptadas antes del inicio.
- Cambiar su contraseña.

---

## 🧭 Reglas de las tutorías

- Las solicitudes nuevas comienzan en **Pendiente**.
- Los estados son **Pendiente, Aceptada, Rechazada, Cancelada y Completada**. Los estados finales no se reabren.
- Las sesiones usan la hora de **Lima, Perú**; deben comenzar en el futuro, durar entre **15 y 180 minutos** y terminar el mismo día.
- Las tutorías pendientes y aceptadas reservan el horario del alumno y del docente. Se impiden cruces y envíos duplicados simultáneos; los horarios contiguos se permiten.
- Los docentes inactivos conservan su historial y no reciben nuevas solicitudes.
- Los registros antiguos sin hora conservan sus datos; no se les asigna un horario inventado.

## 🎨 Interfaz

Portada con especialidades y presentación de la plataforma, navegación según la cuenta, diseño compartido en turquesa, formularios etiquetados, mensajes dentro de la página y tablas adaptadas a móviles.

## 🔐 Seguridad

Contraseñas almacenadas con PBKDF2, controles de acceso por rol y propiedad de la tutoría, protección CSRF y escape de los datos mostrados en las vistas. Las credenciales se configuran mediante variables de entorno y no se guardan en el repositorio.

---

## ⚙️ Instalación y ejecución local

1. Clonar el repositorio e instalar **JDK 17**, **Maven**, **Tomcat 9** y **MySQL 8**.
2. Para una base nueva y vacía, seleccionar la base e importar [schema.sql](database/schema.sql) y, opcionalmente, [catalog.sql](database/catalog.sql).
3. Configurar las variables de conexión y del administrador según la [guía de despliegue](deploy/README.md). Para HTTP local, usar `SESSION_COOKIE_SECURE=false`.
4. Compilar y ejecutar las pruebas:

   ```powershell
   mvn clean package
   ```

5. Copiar `target/TeachMate.war` a `webapps` de Tomcat 9 y abrir [TeachMate local](http://localhost:8080/TeachMate/).

Las pruebas habituales usan una base H2 aislada; no requieren ni modifican tu MySQL. La guía también explica cómo comprobar los datos y la migración con una instancia MySQL local de pruebas.

## ☁️ Despliegue

Render construye la aplicación con el [Dockerfile](Dockerfile) y la configuración de [render.yaml](render.yaml). La base MySQL se aloja por separado, por ejemplo en Aiven, y sus credenciales y certificado CA se configuran en Render. La ruta `/health` comprueba la conexión y el esquema.

**Si la base ya existe:** respaldarla, detener la versión anterior y aplicar [tutorias-migration.sql](database/tutorias-migration.sql) una sola vez antes de publicar el código que utiliza horarios y comentarios separados. No importar el esquema inicial sobre una base existente.

La configuración, actualización de la base, comprobaciones y recuperación están descritas en la [guía de despliegue](deploy/README.md).
