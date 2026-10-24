# TeachMate

Sistema web para un equipo de profesores que ofrece asesorías individuales remotas, coordinado por un administrador. Permite gestionar cursos, disponibilidad, reservas, pagos de alumnos y liquidaciones a profesores.

Utiliza Java 17, Maven, JSP/Servlets, Tomcat 9 y MySQL 8.0.16 o posterior. Los importes se expresan en soles (PEN) y las citas y vencimientos utilizan `America/Lima`.

## Qué contiene el proyecto

| Rol | Funciones |
| --- | --- |
| Administrador | Profesores, cursos, asignaciones, tarifas, condiciones del servicio, revisión de pagos, cancelaciones, devoluciones y liquidaciones. |
| Profesor | Perfil, disponibilidad, respuesta a solicitudes, enlace de videollamada, resultado de la sesión, ingresos y revisión de ajustes. |
| Alumno | Registro, perfil, consulta de cursos y horarios, reservas, comprobantes, seguimiento y solicitudes de cancelación o devolución. |

Un profesor puede impartir varios cursos y cada curso puede tener varios profesores. Las videollamadas, cobros, devoluciones y transferencias se realizan en servicios externos; TeachMate registra su seguimiento.

No incluye clases grupales, múltiples equipos independientes, pasarela bancaria, transferencias automáticas, nómina ni contabilidad fiscal. No hay correos, WhatsApp ni recordatorios automáticos configurados: el equipo debe revisar sus paneles.

## Estructura

```text
TeachMate/
├── src/main/java/       # Acceso a datos, reglas, seguridad y controladores
├── src/main/webapp/     # Pantallas JSP, estilos y navegación
├── database/schema.sql # Instalación completa de una base nueva
├── deploy/
│   ├── start.sh        # Arranque del contenedor y certificado de MySQL
│   └── server.xml      # Configuración de Tomcat
├── .env.example        # Variables de entorno de referencia
├── Dockerfile          # Construcción de la imagen
├── render.yaml         # Configuración del servicio en Render
├── pom.xml             # Dependencias y compilación Maven
└── README.md           # Guía del proyecto
```

`target/` se genera al compilar. Los archivos `.project`, `.classpath` y `.settings/` permiten importar el proyecto en Eclipse; también puede abrirse como proyecto Maven en otro editor. No copiar librerías manualmente a `WEB-INF/lib`.

## Flujo y reglas del servicio

1. Administración registra cursos y profesores, asigna los cursos y configura tarifas, reparto, plazos e instrucciones de cobro. El profesor completa su perfil y disponibilidad.
2. El alumno elige curso, profesor, duración y horario; indica nivel y tema y revisa precio y condiciones antes de solicitar.
3. La solicitud retiene temporalmente las agendas del alumno y del profesor. Solo el profesor asignado acepta o rechaza; aceptar habilita el pago.
4. El alumno transfiere a la cuenta del equipo y adjunta su comprobante. Administración verifica el ingreso real; subir un comprobante no confirma el pago.
5. El pago validado confirma la asesoría. El profesor publica el enlace privado de la videollamada y, al finalizar, registra realización, seguimiento o incidencia.
6. Administración prepara el cierre semanal con las sesiones elegibles y registra la transferencia al profesor después de realizarla externamente.

### Reservas y cambios

- Solo se reserva con profesor activo, curso asignado, disponibilidad, tarifa y configuración comercial completas. No se precargan precios, porcentajes ni cuentas de ejemplo.
- Se impiden cruces de alumno y profesor, incluso en solicitudes simultáneas. Los horarios contiguos se permiten y las retenciones vencidas dejan de bloquear reservas.
- Un comprobante recibido a tiempo conserva el horario durante su revisión. Administración puede observarlo y conceder el plazo de corrección configurado, sin superar el inicio de la clase. Si llega el inicio sin validación, pasa a incidencia.
- Un pago tardío no desplaza otra reserva: se resuelve mediante devolución o una nueva reserva acordada.
- Reprogramar conserva el horario original hasta que ambos participantes aceptan y se vuelve a comprobar disponibilidad. Se conservan precio, reparto y pago, con historial del cambio.
- Las cancelaciones o devoluciones solicitadas después de pagar requieren revisión administrativa. No hay penalización automática de 24 horas ni devolución automática. Si el profesor cancela una sesión pagada sin alternativa aceptada, corresponde tramitar la devolución.
- Inasistencias, reclamos y solicitudes pendientes de resolución bloquean la liquidación de la sesión. Las decisiones conservan responsable, fecha y motivo.
- Desactivar un profesor impide nuevas reservas y conserva las sesiones e historial existentes.

### Pagos y liquidaciones

- Reserva, pago del alumno, realización de la sesión y transferencia al profesor tienen estados separados. Cancelar no acredita devolución; validar un pago no acredita clase realizada ni profesor pagado.
- El precio y reparto se guardan por reserva. Cambiar una tarifa no modifica operaciones anteriores.
- Se liquidan únicamente sesiones realizadas, pagadas y sin asuntos pendientes, una sola vez. Sin sesiones elegibles no se genera remuneración por asesorías.
- Los importes usan decimales exactos. La parte del profesor se redondea con `HALF_UP` a dos decimales; la parte del equipo es el total menos esa parte y no equivale a utilidad neta ni sueldo administrativo.
- Una devolución posterior no reescribe una transferencia histórica. Administración puede asumir el costo o proponer un ajuste; una reducción requiere aceptación del profesor. No se registra una transferencia negativa.

### Privacidad y seguridad

Acceso por rol y propiedad, contraseñas PBKDF2, consultas parametrizadas, protección CSRF y escape HTML. Alumno y profesor consultan sus propias operaciones; los enlaces de clase son privados.

Los comprobantes admiten PDF, JPG o PNG de hasta 5 MB. Se almacenan en MySQL y solo los descargan su alumno o administración; deben incluirse en los respaldos. Las sesiones duran 30 minutos y se renuevan al ingresar o cambiar contraseña. Reiniciar el servicio cierra las sesiones; cambiar contraseña no revoca automáticamente otras sesiones abiertas.

## Instalar y ejecutar localmente

Requisitos: JDK 17, Maven, Tomcat 9 y MySQL 8.0.16 o posterior, versión que aplica las restricciones `CHECK` del esquema.

### 1. Preparar la base de datos

En MySQL Workbench o un cliente equivalente, crear y seleccionar una base nueva:

```sql
CREATE DATABASE teachmateweb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE teachmateweb;
```

Ejecutar [database/schema.sql](database/schema.sql) completo, una sola vez sobre esa base vacía. Crea todas las tablas, restricciones e índices y las tres filas de configuración necesarias. No crea cuentas, cursos, tarifas ni operaciones de prueba.

El script no borra tablas ni actualiza bases existentes. Si la importación falla a mitad, revisar el error y utilizar una base vacía antes de repetirla: MySQL confirma los cambios de estructura por separado. No ejecutar este script sobre una base con datos que se deban conservar.

Crear un usuario de aplicación con permisos `SELECT`, `INSERT`, `UPDATE` y `DELETE` sobre esa base. Usar una cuenta de administración distinta para crear la estructura.

### 2. Compilar y crear el acceso del administrador

```powershell
mvn clean package
java -cp "target/classes;target/TeachMate/WEB-INF/lib/*" com.tme.security.PasswordTool hash
```

La herramienta solicita y confirma la contraseña sin mostrarla. Guardar el resultado en `ADMIN_PASSWORD_HASH`; nunca utilizar la contraseña en texto plano. En PowerShell, usar comillas simples al asignar un hash que contiene `$`. En Linux/macOS, sustituir el separador `;` del classpath por `:`.

Maven genera `target/TeachMate.war`. Actualmente no hay una suite de pruebas automatizadas incluida; compilar no sustituye comprobar la aplicación con MySQL y Tomcat.

### 3. Configurar el entorno

Usar [.env.example](.env.example) como referencia. La aplicación no carga ese archivo automáticamente: configurar las variables en el proceso que inicia Tomcat o en **Environment** de Render.

| Variable | Uso |
| --- | --- |
| `DB_URL` | URL JDBC de MySQL, sin credenciales incorporadas. |
| `DB_USER`, `DB_PASSWORD` | Usuario de aplicación y contraseña. |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD_HASH` | Acceso del administrador, configurado por entorno. |
| `DB_CA_CERT_BASE64` | CA pública de Aiven en Base64, en una sola línea; vacía para MySQL local sin TLS. |
| `SESSION_COOKIE_SECURE` | `false` para HTTP local; `true` para HTTPS en Render. |
| `TRUST_RENDER_PROXY` | `false` en local; `true` únicamente para el servicio público de Render. |
| `PORT` | Puerto del contenedor; valor predeterminado `10000`. Render lo proporciona. |

Ejemplo de conexión local:

```text
jdbc:mysql://localhost:3306/teachmateweb?connectionTimeZone=UTC&connectTimeout=10000&socketTimeout=30000
```

No guardar contraseñas reales, archivos `.env`, claves privadas ni respaldos en el repositorio.

### 4. Iniciar Tomcat

Copiar `target/TeachMate.war` a `webapps/` de Tomcat e iniciarlo con las variables configuradas.

- Aplicación: `http://localhost:8080/TeachMate/`
- Comprobación: `http://localhost:8080/TeachMate/health`

`/health` devuelve HTTP 200 y `{"status":"ok"}` si puede conectarse y consultar la estructura requerida, o HTTP 503 si falla. No acredita que las condiciones comerciales estén completas ni sustituye probar las operaciones.

## Publicar en Render con MySQL en Aiven

El proyecto incluye [la dirección del servicio TeachMate](https://teachmate-7wu8.onrender.com). Comprobar en Render el repositorio, rama y commit utilizados para conocer qué versión está publicada.

1. Preparar una base vacía en Aiven e importar `database/schema.sql`. Para sustituir una base anterior, detener primero las escrituras, confirmar que no hay información que conservar y guardar un respaldo antes del cambio.
2. Configurar las variables en Render con los datos reales de Aiven. Usar una conexión como esta:

   ```text
   jdbc:mysql://HOST:PUERTO/BASE?sslMode=VERIFY_IDENTITY&connectionTimeZone=UTC&connectTimeout=3000&socketTimeout=3000
   ```

3. Configurar la CA pública en `DB_CA_CERT_BASE64`, `SESSION_COOKIE_SECURE=true` y `TRUST_RENDER_PROXY=true`. El arranque del contenedor prepara el truststore temporal; no desactivar la verificación TLS.
4. Usar el `Dockerfile` y la ruta de salud `/health`. El contenedor ya incluye su comando de arranque; no necesita otro Start Command. En el contenedor la aplicación abre en `/`.
5. Revisar **Auto-Deploy: Off** en el servicio existente y desplegar manualmente el commit elegido. `render.yaml` también define los despliegues manuales. La estructura se prepara antes del código; el arranque no ejecuta SQL.
6. Esperar `/health` con HTTP 200 y comprobar acceso, navegación y operaciones de los tres roles antes de abrir el servicio.

La conexión puede trabajar en UTC; la agenda utiliza fechas y horas locales de `America/Lima`. El contenedor escucha en `0.0.0.0:$PORT` y se ejecuta como usuario sin privilegios.

### Probar el contenedor localmente

Con Docker instalado y un archivo `.env` real configurado:

```powershell
docker build -t teachmate .
docker run --rm --env-file .env -e SESSION_COOKIE_SECURE=false -e TRUST_RENDER_PROXY=false -p 10000:10000 teachmate
```

Abrir `http://localhost:10000/`. Para conectarse desde Docker Desktop a MySQL en Windows puede utilizarse `host.docker.internal` en `DB_URL`. Los valores de cookies y proxy del ejemplo corresponden a HTTP local.

## Preparar el servicio y verificar cambios

Antes de recibir alumnos, administración debe configurar cursos, profesores, tarifas por duración, porcentaje de reparto, plazos de respuesta/pago/corrección, antelación mínima, cuenta receptora, instrucciones de cobro, contacto del equipo y días de corte y transferencia. Definir las políticas de inasistencia y los niveles atendidos; si se admiten menores, definir también el responsable de la cuenta y la comunicación con su representante.

Comprobar en un entorno de ensayo:

- Acceso y permisos de los tres roles, también desde celular.
- Oferta, disponibilidad, reservas simultáneas, cruces y vencimientos.
- Aceptación, comprobante privado, observación, corrección y validación del pago.
- Enlace privado, reprogramación con ambas aceptaciones, realización e incidencias.
- Cancelación, devolución y liquidación sin duplicados, incluidos los ajustes que requieren aceptación.

No crear pagos ficticios en la base publicada. Respaldar estructura y datos, incluidos los comprobantes, con las herramientas de Aiven o `mysqldump` con TLS verificado. Comprobar la restauración en otra base; en PowerShell, usar `--result-file` con `mysqldump` para conservar la codificación. No pasar contraseñas en la línea de comandos.

Esta instalación es el punto de partida del proyecto. Cuando existan datos reales, los cambios de estructura deberán planificarse para conservarlos. Volver al código anterior no revierte MySQL; una recuperación debe coordinar código, configuración y base de datos, conservando las operaciones nuevas que se hayan registrado.
