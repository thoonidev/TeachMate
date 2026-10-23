# Guía de despliegue de TeachMate

## Organización del proyecto

| Ubicación | Contenido |
| --- | --- |
| `src/main/java` | Servlets, acceso a datos, modelos, seguridad y salud. |
| `src/main/webapp` | Páginas JSP, estilos, imágenes y video. |
| `src/test/java` | Pruebas automáticas de conexiones, seguridad y configuración. |
| `database` | Esquema MySQL, catálogo inicial y migración de instalaciones antiguas. |
| `deploy` | Esta guía, el arranque del contenedor y la configuración de Tomcat. |
| `pom.xml` | Compilación con Maven y dependencias. |
| `Dockerfile` / `render.yaml` | Construcción de la imagen y servicio de Render. |
| `.env.example` | Nombres de las variables; no contiene credenciales reales. |

`target` se genera al compilar y no se versiona. Las librerías las descarga Maven;
no se necesitan JAR copiados en `WEB-INF/lib`. En Eclipse, importar como **Existing
Maven Projects** y ejecutar **Maven > Update Project**. Usar Java 17 y Tomcat 9.

## Compilar y comprobar

```powershell
mvn clean package
```

Ejecuta las pruebas y genera `target/TeachMate.war`. Las pruebas usan Tomcat 9 y
una base H2 aislada en memoria; no acceden a tu MySQL. Las dependencias de pruebas
no se incluyen en el WAR. Para ejecutar sin Docker, copiar el WAR a `webapps` de
Tomcat 9 y abrir `http://localhost:8080/TeachMate/`.

Para repetir las pruebas de datos, migración y concurrencia con **MySQL 8 real**,
usar una instancia local dedicada a pruebas y su puerto:

```powershell
mvn "-Dteachmate.test.mysql.port=PUERTO" "-Dtest=TutoriaDataTest,TutoriaReglasTest" test
```

Estas pruebas solo conectan a `127.0.0.1`, crean bases con nombres aleatorios
`teachmate_validation_...` y las eliminan al terminar. Utilizan `root` de esa
instancia de pruebas; si tiene contraseña, configurarla en la variable temporal
`TEACHMATE_TEST_MYSQL_PASSWORD` de la terminal, sin escribirla en Git ni en el chat.
No usar el puerto de una base de trabajo o producción. La opción no se activa en
la compilación habitual ni durante la construcción de Render.
La instancia de pruebas debe admitir TLS: estas conexiones exigen cifrado.

## Base de datos

Crear una base MySQL 8 vacía con `utf8mb4`, seleccionarla en el cliente SQL e
importar `database/schema.sql`. Opcionalmente importar `database/catalog.sql`
para cargar cinco especialidades sin crear cuentas ni duplicar el catálogo.
El esquema de instalación nueva se ejecuta una sola vez.

Para Render se necesita una base MySQL remota accesible desde la región elegida.
El Blueprint solo crea la aplicación; no crea MySQL. Usar un usuario de aplicación
con SELECT, INSERT, UPDATE y DELETE. Importar el esquema con otra cuenta de
administración y autorizar la conexión en las reglas del proveedor.

El respaldo original se conserva localmente como `database/original-backup.sql`,
ignorado por Git y Docker. No importarlo sobre datos existentes: incluye DROP DATABASE.

### Instalaciones con cuentas antiguas

1. Respaldar la base y detener la aplicación.
2. Revisar y resolver correos duplicados en `user_details` y `docente`:

   ```sql
   SELECT email, COUNT(*) FROM user_details GROUP BY email HAVING COUNT(*) > 1;
   SELECT email, COUNT(*) FROM docente GROUP BY email HAVING COUNT(*) > 1;
   ```

3. Ejecutar `database/security-migration.sql` una vez sobre esa base. Amplía las
   columnas de claves y agrega correos únicos. No se necesita para bases creadas
   con el esquema actual. MySQL confirma estos cambios de estructura por separado;
   si falla una instrucción, revisar qué se aplicó antes de repetir.
4. Configurar `DB_URL`, `DB_USER` y `DB_PASSWORD` en la terminal y ejecutar:

   ```powershell
   java -cp "target/classes;target/TeachMate/WEB-INF/lib/*" com.tme.security.PasswordTool migrate
   ```

La conversión de contraseñas es transaccional y puede repetirse: conserva los
hashes válidos existentes y revierte las conversiones si falla. Mantiene el acceso
con las claves antiguas; el próximo cambio exige de 12 a 128 caracteres. El inicio
de sesión ya no acepta claves guardadas en texto plano. Un resultado de cero es
válido si no hay cuentas. Proteger el respaldo, que todavía contiene claves antiguas.

## Actualización de tutorías en una base existente

Antes de publicar el código que usa comentarios, horarios y docentes activos:
respaldar MySQL, detener la versión anterior y ejecutar una sola vez
`database/tutorias-migration.sql`. Después publicar la aplicación y comprobar
`/health`. La migración no se ejecuta automáticamente durante el arranque.
No aplicar este script a una base creada con el `schema.sql` actual.

El formato anterior guardaba el comentario dentro de `estado`: los valores
pendientes o vacíos se conservan como pendientes; el resto se copia a `comentario`
y se considera completado, siguiendo el comportamiento anterior. Ese texto puede
coincidir con un nombre de estado nuevo y también se conserva como comentario.
No hay información suficiente para reconstruir la hora: las filas antiguas quedan
con horario «Por definir». Revisar los casos ambiguos contra el respaldo.

Los estados admitidos son Pendiente, Aceptada, Rechazada, Cancelada y Completada.
Desde Pendiente se puede aceptar, rechazar o cancelar; desde Aceptada se puede
completar o cancelar. Los estados finales no se reabren. Guardar un comentario no
ejecuta una transición. La desactivación de un docente conserva sus tutorías sin
cambiar su estado, lo retira del selector y revoca sus accesos.

Las nuevas solicitudes requieren una fecha y hora futuras en `America/Lima`,
duración de 15 a 180 minutos y finalización el mismo día. Pendiente y Aceptada
reservan el horario del alumno y del docente; las demás liberan ese horario.
Los intervalos contiguos se permiten. El registro comprueba las dos agendas dentro
de una transacción, incluso ante envíos simultáneos.

El docente asignado acepta o rechaza solicitudes pendientes, cancela antes del
inicio y completa las aceptadas a partir de su hora de finalización. El alumno
solo cancela sus propias tutorías antes del inicio. El administrador puede cancelar
pendientes o aceptadas, incluso iniciadas o de docentes inactivos, para resolver
incidencias. Cada acción se valida en el servidor y requiere sesión y CSRF.
Las solicitudes antiguas sin horario se pueden rechazar o cancelar, pero no aceptar
ni completar; no se inventan horas ni se reabren estados finales.

### Actualizar un servicio que ya está publicado

1. Preparar y comprobar el nuevo código en una rama distinta de la desplegada.
   Subir esa rama a GitHub permite revisar los cambios sin actualizar el servicio.
2. Guardar un respaldo completo de la base existente, incluidos estructura y datos.
   Conservar también el identificador del commit que ejecuta Render.
3. Desactivar temporalmente el despliegue automático y detener el servicio anterior
   antes de modificar el esquema. Así no escribirá comentarios en la columna
   `estado` mientras se actualiza la base.
4. En MySQL Workbench, seleccionar expresamente la base y comprobar:

   ```sql
   SELECT DATABASE();
   SELECT 'docente' AS tabla, COUNT(*) AS filas FROM docente
   UNION ALL SELECT 'user_details', COUNT(*) FROM user_details
   UNION ALL SELECT 'tutoria', COUNT(*) FROM tutoria
   UNION ALL SELECT 'especialidad', COUNT(*) FROM especialidad;
   SELECT table_name, column_name
   FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND ((table_name = 'docente' AND column_name = 'activo')
       OR (table_name = 'tutoria'
           AND column_name IN ('comentario', 'hora_inicio', 'duracion_minutos')));
   ```

   El resultado de columnas debe estar vacío para el esquema anterior. Si aparecen
   columnas nuevas, revisar qué migración ya se aplicó; no repetir el script.
   Para una instalación de Aiven que usa `defaultdb`, comprobar ese nombre en
   `SELECT DATABASE()`.
5. Aplicar `database/tutorias-migration.sql` una sola vez. Comparar los cuatro
   totales con los anteriores y comprobar:

   ```sql
   SELECT estado, COUNT(*) AS filas FROM tutoria GROUP BY estado;
   SELECT COUNT(*) AS horarios_por_definir
   FROM tutoria WHERE hora_inicio IS NULL AND duracion_minutos IS NULL;
   ```

6. Integrar la rama revisada en la rama que utiliza Render y desplegar ese commit.
   Comprobar `/health`, los accesos de los tres roles, una solicitud futura,
   aceptación, comentarios y cancelación. La finalización se habilita cuando
   termina la sesión; no cambiar las fechas de datos reales para probarla.
7. Restaurar el despliegue automático cuando la comprobación termine.

Si falla la migración, mantener el servicio detenido y revisar cada instrucción:
MySQL no revierte automáticamente los cambios de estructura anteriores al fallo.
Para volver a la versión anterior, conservar primero una copia de los datos
actuales y restaurar el respaldo previo junto con el código anterior. No basta con
desplegar el WAR antiguo sobre el esquema nuevo.

## Administrador y variables

Generar el hash del administrador desde una terminal interactiva:

```powershell
java -cp "target/classes;target/TeachMate/WEB-INF/lib/*" com.tme.security.PasswordTool hash
```

Solicita y confirma la contraseña sin mostrarla. Copiar el resultado a
`ADMIN_PASSWORD_HASH`; nunca usar una contraseña sin hash ni los datos de pruebas.
En PowerShell encerrar el hash entre comillas simples por los signos `$`.

| Variable | Configuración |
| --- | --- |
| `DB_URL` | URL JDBC de MySQL, sin usuario ni contraseña incorporados. |
| `DB_USER`, `DB_PASSWORD` | Credenciales del usuario de aplicación. |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD_HASH` | Acceso del administrador. |
| `PORT` | `10000` por defecto; el contenedor respeta el puerto configurado. |
| `SESSION_COOKIE_SECURE` | `true` en Render; `false` en HTTP local. |
| `TRUST_RENDER_PROXY` | `true` en el servicio público de Render; `false` en acceso local directo. |
| `DB_CA_CERT_BASE64` | Certificado CA de Aiven codificado en Base64, en una sola línea. Vacío para MySQL local. |

Ejemplo de URL remota, reemplazando host, puerto y base:

```text
jdbc:mysql://HOST:3306/BASE?sslMode=VERIFY_IDENTITY&connectionTimeZone=UTC&connectTimeout=3000&socketTimeout=3000
```

Para Aiven, convertir el certificado descargado desde PowerShell (sustituir la ruta):

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes('C:\ruta\ca.pem'))
```

Copiar el resultado completo a `DB_CA_CERT_BASE64` en Render. El arranque crea un
almacén de confianza temporal para Java a partir de ese certificado, sin modificar
el repositorio ni incluirlo en la imagen. Su contraseña fija protege solo certificados
públicos; no contiene claves privadas ni la contraseña de MySQL. Usar siempre
`sslMode=VERIFY_IDENTITY` en la URL de Aiven para comprobar también el hostname.

Configurar los certificados según el proveedor sin desactivar la validación TLS.
Introducir los secretos en Render y no guardarlos
en Git. `.env.example` es una referencia; Tomcat no carga `.env` automáticamente.

## Docker y Render

Con Docker instalado, crear un `.env` local con valores reales y ejecutar:

```powershell
docker build -t teachmate .
docker run --rm --env-file .env -e SESSION_COOKIE_SECURE=false -e TRUST_RENDER_PROXY=false -p 10000:10000 teachmate
```

Si MySQL corre en Windows con Docker Desktop, usar `host.docker.internal` en la
URL y permitir esa conexión en MySQL. Dentro del contenedor, `localhost` se refiere
al propio contenedor. Abrir `http://localhost:10000/` y `/health`.

Para publicar:

1. Revisar los cambios, verificar que no haya secretos y subirlos a GitHub.
2. Preparar MySQL remoto y los accesos descritos arriba.
3. Crear en Render un **Blueprint** conectado al repositorio y la rama deseada.
4. Revisar el plan y la región. `render.yaml` propone Free para una demostración.
5. Completar las seis variables solicitadas y crear el servicio. Render construye
   el Dockerfile; el arranque está incluido y no requiere otro Start Command.
6. Esperar que `/health` responda 200 y comprobar los flujos de los tres roles.

También se puede crear manualmente un **Web Service** con lenguaje **Docker**,
Dockerfile `./Dockerfile`, contexto `.` y salud `/health`. La aplicación abre en
`https://NOMBRE.onrender.com/`, sin `/TeachMate`. Cada commit en la rama vinculada
activa un despliegue; puede cambiarse en Render. Revisar los límites del plan Free.

## Comportamiento en producción

- La imagen usa Java 17 y Tomcat 9, escucha en `0.0.0.0:$PORT` y ejecuta la app
  con un usuario sin privilegios. No incluye Maven, pruebas ni respaldos.
- `/health` comprueba MySQL y las cuatro tablas: devuelve 200 con estado `ok` o
  503 con estado `unavailable`, sin revelar errores SQL ni crear sesiones.
- Las cookies usan HttpOnly y SameSite=Lax. Las sesiones expiran tras 30 minutos
  sin actividad y se renuevan al ingresar o cambiar contraseña. Otras sesiones
  abiertas no se revocan automáticamente al cambiar la contraseña.
- `TRUST_RENDER_PROXY` habilita CF-Connecting-IP y X-Forwarded-Proto desde los
  saltos internos reconocidos por Tomcat. No habilitarlo con otro proxy sin adaptar
  la configuración. Los accesos y registros se limitan por dirección en memoria.
- Reiniciar o desplegar elimina las sesiones: los usuarios vuelven a ingresar.
  Los datos permanecen en MySQL externo; el arranque no modifica el esquema.

Las pruebas automáticas no sustituyen construir y ejecutar la imagen completa ni
validar la aplicación publicada con MySQL real.

Referencias: [Docker en Render](https://render.com/docs/docker),
[Web Services](https://render.com/docs/web-services),
[Blueprints](https://render.com/docs/blueprint-spec),
[IP del cliente](https://render.com/articles/host-pocketbase-on-render),
[almacenamiento de contraseñas](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html).
