# Despliegue, actualización y recuperación

## Archivos y entorno

- `src/main/java`: reglas, acceso a datos, seguridad y Servlets.
- `src/main/webapp`: JSP, estilos y navegación; no contiene comprobantes públicos.
- `database`: esquema nuevo, catálogo opcional, migraciones y comprobación de estructura.
- `deploy/start.sh` y `deploy/tomcat/server.xml`: arranque y configuración del contenedor.
- `Dockerfile`, `render.yaml`, `pom.xml`: construcción y servicio.

Java 17, Maven, Tomcat 9 y MySQL 8. `target` se genera al compilar y se excluye de Git. Importar como proyecto Maven si se usa Eclipse. No copiar librerías manualmente a `WEB-INF/lib`.

```powershell
mvn clean package
```

Genera `target/TeachMate.war`. Los archivos de pruebas temporales de las fases se retiraron por solicitud del responsable; este comando no ejecuta las suites eliminadas ni modifica MySQL.

## Variables de entorno

La aplicación no carga `.env` automáticamente. Configurar las variables en el proceso de Tomcat o en **Environment** del servicio de Render.

| Variable | Uso |
| --- | --- |
| `DB_URL` | URL JDBC MySQL, sin credenciales incorporadas. |
| `DB_USER`, `DB_PASSWORD` | Usuario de aplicación y contraseña. |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD_HASH` | Acceso del administrador. Nunca usar una contraseña sin hash. |
| `DB_CA_CERT_BASE64` | Certificado CA público de Aiven codificado en Base64, en una línea. |
| `SESSION_COOKIE_SECURE` | `true` en HTTPS de Render; `false` para HTTP local. |
| `TRUST_RENDER_PROXY` | `true` únicamente en el servicio público de Render; `false` en local. |
| `PORT` | Render lo proporciona; valor inicial del contenedor: 10000. |

Para Aiven, usar el hostname, puerto y nombre de base reales, con una URL de esta forma:

```text
jdbc:mysql://HOST:PUERTO/BASE?sslMode=VERIFY_IDENTITY&connectionTimeZone=UTC&connectTimeout=3000&socketTimeout=3000
```

`America/Lima` gobierna las citas y vencimientos. La conexión puede usar UTC: los campos locales de agenda se leen y escriben como `LocalDateTime`.

El arranque crea un truststore temporal a partir de la CA, sin incorporarla a la imagen. No desactivar la verificación TLS para resolver problemas de conexión. No guardar claves, certificados privados, respaldos ni `.env` reales en Git, en comentarios o en el chat.

El usuario de aplicación necesita SELECT, INSERT, UPDATE y DELETE sobre su base. Las migraciones requieren una cuenta de administración distinta, con los permisos de estructura y rutinas que utilizan los scripts.

### Administrador

Después de compilar, ejecutar en una terminal interactiva:

```powershell
java -cp "target/classes;target/TeachMate/WEB-INF/lib/*" com.tme.security.PasswordTool hash
```

Solicita y confirma la contraseña sin mostrarla. Copiar el hash a `ADMIN_PASSWORD_HASH`. En PowerShell, usar comillas simples si se asigna un hash con signos `$`. No usar valores de las comprobaciones temporales como credenciales reales.

## Instalación nueva

1. Crear una base MySQL 8 vacía con `utf8mb4` y seleccionarla expresamente.
2. Importar `database/schema.sql` una sola vez. Ya contiene las estructuras de las fases 1 a 6; **no** ejecutar después las migraciones sobre esa misma base.
3. Opcionalmente importar `database/catalog.sql`: crea cursos, sin cuentas, tarifas ni porcentajes de ejemplo.
4. Configurar las variables y publicar el código validado.
5. Comprobar `/health` y completar la preparación comercial desde administración.

## Base existente: comprobar antes de cambiar

Ejecutar `database/verificar-despliegue.sql` con la base seleccionada. Registrar versión, cantidades de alumnos, profesores, asesorías y cursos, estados e inventario de columnas y restricciones.

Los marcadores del script sirven para localizar qué migraciones revisar; un marcador aislado no prueba que un script que falló a mitad haya terminado. Comparar el inventario con cada migración. Si aparecen cambios parciales, detenerse y resolverlos antes de continuar. MySQL confirma DDL por separado: no hay una reversión automática de toda la cadena.

| Orden | Script | Aplicarlo solo si falta su cambio completo |
| --- | --- | --- |
| Previo | `security-migration.sql` | Cuentas antiguas sin columnas de claves ampliadas y correos únicos. Resolver duplicados primero. |
| 1 | `tutorias-migration.sql` | Instalación anterior a comentarios separados, horarios y profesores activos. |
| 2 | `oferta-migration.sql` | Cursos relacionados, presentación, perfiles, tarifas y disponibilidad. |
| 3 | `gestion-migration.sql` | Plazos, enlace, asistencia, cambios e historial de asesorías. |
| 4 | `pagos-migration.sql` | Configuración de cobro, comprobantes, pagos y devoluciones. |
| 5 | `liquidaciones-migration.sql` | Cierres, transferencias, asignaciones únicas y ajustes. |

En una instalación que ya tenga las mejoras anteriores a estas fases, es posible que los pasos previo y 1 ya estén aplicados. No repetirlos: la migración original de tutorías interpreta el antiguo `estado` como comentario y no debe ejecutarse sobre estados modernos.

`oferta-migration.sql` utiliza `DELIMITER` y una rutina temporal de comprobación; ejecutarlo como script en un cliente compatible, por ejemplo MySQL Workbench o `mysql`. Si detecta cursos duplicados, resolverlos contra el respaldo antes de modificar la estructura. Conserva la especialidad anterior como asignación de curso y después retira la columna de texto.

Las filas anteriores no reciben precios, porcentajes, horarios ni pagos inventados. Los registros `Aceptada` conservan su carácter anterior; las solicitudes sin instrucciones de cobro se revisan explícitamente desde administración. Las devoluciones ejecutadas de sesiones completadas generan ajustes pendientes de decisión, sin transferencias ficticias.

### Contraseñas de instalaciones antiguas

Si todavía hay claves sin hash, después de la migración de seguridad configurar temporalmente la conexión de administración en una terminal local y ejecutar:

```powershell
java -cp "target/classes;target/TeachMate/WEB-INF/lib/*" com.tme.security.PasswordTool migrate
```

La conversión es transaccional y conserva hashes válidos. Repetirla no vuelve a convertirlos. El acceso ya no acepta texto plano. Proteger especialmente los respaldos anteriores a esa conversión.

## Actualizar el servicio de Render existente

Servicio público: [TeachMate](https://teachmate-7wu8.onrender.com). Confirmar en **Settings** el repositorio, la rama y el commit desplegado; la rama local abierta en Git Bash no determina la rama de Render.

1. Revisar el commit de entrega en una rama que no active la producción. Confirmar que no incluye datos de prueba ni secretos.
2. En Render, poner **Auto-Deploy: Off** antes de integrar o subir a la rama enlazada. El `render.yaml` nuevo también usa `autoDeployTrigger: 'off'`, pero no sustituye revisar la configuración del servicio existente.
3. Preparar una ventana de mantenimiento y detener el acceso y las escrituras de la versión anterior. No confiar únicamente en `/health`: Render puede mantener la instancia anterior atendiendo durante un despliegue.
4. Guardar un respaldo completo y consistente de Aiven, con estructura, datos, comprobantes y registros financieros. Conservar también el commit anterior y la configuración de servicio en un lugar seguro. Verificar la restauración en un MySQL separado **antes** de aplicar cambios a producción.
5. Confirmar `SELECT DATABASE()`, ejecutar la comprobación de estructura y aplicar únicamente las migraciones pendientes, en orden. Usar la cuenta de administración; no ejecutar `schema.sql` ni importar el respaldo original sobre datos existentes.
6. Comparar cantidades, cuentas, estados e historial. Comprobar las nuevas restricciones y estructuras. Los cursos pueden aumentar al conservar especialidades anteriores; alumnos, profesores y asesorías deben conservarse.
7. Desde Render, desplegar expresamente el commit revisado, sin dejar que la versión antigua vuelva a escribir sobre el nuevo esquema. Si se utilizó suspensión u otra medida de mantenimiento, revisar su estado y la versión que arrancará antes de restablecer el servicio.
8. Esperar `/health` = 200 y `{"status":"ok"}`. Comprobar los accesos de los tres roles, navegación, cursos, horarios y condiciones reales. Probar cobros y liquidaciones con datos ficticios únicamente en una instalación de ensayo; no fabricar pagos en producción.
9. Restablecer el acceso cuando el recorrido esté confirmado. Registrar commit, fecha, scripts aplicados y resultado de las comprobaciones. Mantener los despliegues manuales si las próximas entregas también requieren migraciones.

No activar un deploy que dependa de tablas nuevas antes de aplicar su estructura. Si no existe acceso administrativo, respaldo verificado o confirmación de la rama de Render, la entrega queda preparada para publicar, no publicada.

### Respaldo y recuperación

Usar las herramientas de Aiven o `mysqldump` con TLS y CA verificados. No pasar contraseñas en la línea de comandos; usar un perfil de conexión protegido o entrada interactiva. En PowerShell, usar `--result-file` al generar el respaldo para evitar cambiar la codificación del SQL mediante redirección de texto.

La comprobación de restauración debe hacerse en otra instancia o base expresamente destinada al ensayo. Verificar cuentas, agenda, estados, referencias y contenido binario de comprobantes. No crear datos de ensayo en la base publicada.

Si una migración falla, mantener las escrituras detenidas y revisar las instrucciones ya confirmadas. Para volver al esquema anterior, conservar primero una copia del estado actual y restaurar el respaldo previo junto con el código y configuración anteriores. **Volver al commit anterior en Render no revierte MySQL.**

Si ya se admitieron operaciones después de la actualización, recuperar y conciliar esos movimientos antes de restaurar un respaldo anterior: no descartar pagos o reservas nuevas.

## Comprobación operativa de salida

- Administración: accesos, profesores/cursos, tarifas y reparto reales, plazos, cuenta receptora, contacto e instrucciones y días de corte/pago completos.
- Profesor: varios cursos, disponibilidad, aceptación/rechazo, enlace privado, realización/incidencia e ingresos propios.
- Alumno: registro, perfil, selección de horario, solicitud y seguimiento, pago habilitado solo después de aceptar y privacidad del comprobante.
- Ensayo: reservas simultáneas, vencimientos, comprobante observado/corregido, confirmación, reprogramación con ambas aceptaciones, cancelación y devolución con movimientos separados.
- Ensayo: sesión realizada y pagada entra una vez en el cierre; sin actividad no hay remuneración. Un cierre pendiente no acredita transferencia. Ajustes negativos requieren aceptación y no reescriben pagos anteriores.
- Celular: menú, formularios, pagos, enlace de clase e ingresos sin desplazamiento horizontal obligatorio.

No hay correo ni recordatorio automático configurado: el equipo debe revisar sus paneles e historiales. Los comprobantes permanecen en MySQL; incluirlos en respaldos y controlar el crecimiento y los límites del plan de Aiven.

## Contenedor y diagnóstico

Con Docker disponible:

```powershell
docker build -t teachmate .
docker run --rm --env-file .env -e SESSION_COOKIE_SECURE=false -e TRUST_RENDER_PROXY=false -p 10000:10000 teachmate
```

Para MySQL en Windows desde Docker Desktop, el host puede ser `host.docker.internal`; permitir únicamente el acceso necesario. La aplicación del contenedor abre en `/`, sin `/TeachMate`. El ejemplo local cambia las cookies y el proxy porque usa HTTP directo; no trasladar esos valores a Render HTTPS.

El arranque valida variables, configura el truststore y escucha en `0.0.0.0:$PORT` como usuario sin privilegios. No modifica el esquema ni incorpora respaldos, comprobantes o herramientas temporales. No necesita otro Start Command en Render.

`/health` no crea sesiones ni devuelve datos de conexión o clientes. Verifica tablas y columnas en una consulta sin filas, para reducir viajes a Aiven. Configuración comercial incompleta mantiene las reservas deshabilitadas, pero no provoca un falso fallo de infraestructura.

Cookies HttpOnly, SameSite=Lax y Secure en Render. Sesiones de 30 minutos, renovadas al ingresar y cambiar contraseña. Reiniciar el servicio cierra las sesiones; los datos permanecen en MySQL. Cambiar contraseña no revoca automáticamente otras sesiones ya abiertas.

La validación con Tomcat/MySQL locales no equivale a haber construido la imagen Docker ni comprobado Aiven o Render. Registrar por separado qué comprobaciones se pudieron ejecutar en cada entorno.

Referencias: [despliegues de Render](https://render.com/docs/deploys), [comprobaciones de salud](https://render.com/docs/health-checks), [Blueprint](https://render.com/docs/blueprint-spec), [TLS de Aiven](https://aiven.io/docs/platform/concepts/tls-ssl-certificates) y [respaldo y restauración MySQL](https://aiven.io/docs/products/mysql/howto/migrate-database-mysqldump).