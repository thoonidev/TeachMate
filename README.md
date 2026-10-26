# TeachMate

Sistema web para un equipo de profesores que da asesorías individuales remotas, coordinado por un administrador. Gestiona cursos, disponibilidad, reservas, pagos y liquidaciones. Stack: Java 17, Maven, JSP/Servlets, Tomcat 9, MySQL 8.0.16+. Importes en PEN; horarios en `America/Lima`.

## Roles

| Rol           | Funciones                                                                                                 |
| ------------- | --------------------------------------------------------------------------------------------------------- |
| Administrador | Profesores, cursos, tarifas, condiciones, revisión de pagos, cancelaciones, devoluciones y liquidaciones. |
| Profesor      | Perfil, disponibilidad, reservas, enlace de videollamada, resultado de sesión e ingresos.                 |
| Alumno        | Registro, reservas, comprobantes y solicitudes de cancelación o devolución.                               |

Videollamadas, cobros y transferencias son externos; TeachMate solo registra el seguimiento. Incluye avisos opcionales por correo mediante Brevo. No incluye clases grupales, pasarela de pago, transferencias automáticas, nómina, contabilidad ni WhatsApp.

## Estructura

```text
TeachMate/
├── src/main/java/       # Datos, reglas, seguridad y controladores
├── src/main/webapp/     # JSP y estilos
├── database/schema.sql  # Base nueva completa
├── deploy/              # start.sh y server.xml
├── .env.example · Dockerfile · render.yaml · pom.xml
```

## Flujo

1. Administración configura cursos, profesores, tarifas, reparto y plazos; el profesor define su disponibilidad.
2. Publicar disponibilidad compromete al profesor a atender reservas de sus cursos en esas franjas; no se requiere aceptación individual.
3. El alumno elige curso, profesor y horario; la reserva pasa directamente a pendiente de pago, con vencimiento desde su creación.
4. El alumno transfiere y sube comprobante; administración valida el ingreso real.
5. Pago validado = asesoría confirmada. El profesor publica el enlace y registra el resultado.
6. Administración cierra cada semana y registra la transferencia al profesor.

## Reglas clave

- Reserva, pago, realización y transferencia al profesor son estados separados.
- No se permiten cruces de agenda; las retenciones vencidas liberan el horario.
- Antelación mínima de 24 horas (1440 minutos), configurable hacia arriba y suficiente para cubrir el plazo de pago. Retirar disponibilidad no cancela reservas existentes.
- Se ofrecen horarios desde el mayor de estos límites: antelación mínima o fin del plazo de pago más el margen de revisión en horas de atención. Atención inicial: lunes a sábado, 09:00–18:00 Lima; revisión: 120 minutos. El pago corre en tiempo continuo; la revisión salta noches y días sin atención. Cada reserva conserva sus condiciones; las correcciones también dejan margen para revisar antes de la clase. Calendario semanal, sin feriados automáticos.
- Comprobante a tiempo conserva el horario; si no se valida al inicio, pasa a incidencia. Pago tardío: devolución o nueva reserva.
- Reprogramar requiere aceptación de ambos. Cancelaciones y devoluciones tras pagar requieren revisión administrativa (nada automático).
- Precio y reparto se guardan por reserva. Solo se liquidan sesiones realizadas, pagadas y sin pendientes, una sola vez. Parte del profesor con `HALF_UP` a 2 decimales.
- Una devolución posterior no reescribe transferencias pasadas ni genera transferencias negativas; los ajustes a la baja requieren aceptación del profesor.
- Seguridad: acceso por rol y propiedad, PBKDF2, CSRF, consultas parametrizadas. Comprobantes PDF/JPG/PNG ≤ 5 MB en MySQL (incluir en respaldos). Sesiones de 30 min.

## Instalación local

Requisitos: JDK 17, Maven, Tomcat 9, MySQL 8.0.16+.

1. Crear base `teachmateweb` (utf8mb4) y ejecutar `database/schema.sql` una sola vez sobre base vacía. Usar un usuario de aplicación con `SELECT`, `INSERT`, `UPDATE`, `DELETE`.
2. Compilar y generar el hash del administrador:

   ```powershell
   mvn clean package
   java -cp "target/classes;target/TeachMate/WEB-INF/lib/*" com.tme.security.PasswordTool hash
   ```

3. Configurar variables de entorno (no se carga `.env` solo):

   | Variable                                      | Uso                                |
   | --------------------------------------------- | ---------------------------------- |
   | `DB_URL`, `DB_USER`, `DB_PASSWORD`            | Conexión a MySQL                   |
   | `ADMIN_EMAIL`, `ADMIN_PASSWORD_HASH`          | Acceso del administrador           |
   | `DB_CA_CERT_BASE64`                           | CA de Aiven (vacía en local)       |
   | `SESSION_COOKIE_SECURE`, `TRUST_RENDER_PROXY` | `false` en local, `true` en Render |
   | `PORT`                                        | Por defecto `10000`                |

4. Copiar `target/TeachMate.war` a `webapps/` de Tomcat. App: `http://localhost:8080/TeachMate/` · Salud: `/health`.

## Publicar en Render + Aiven

1. Importar `schema.sql` en una base vacía de Aiven.
2. Configurar variables con `sslMode=VERIFY_IDENTITY` en `DB_URL`, más `SESSION_COOKIE_SECURE=true` y `TRUST_RENDER_PROXY=true`.
3. Usar el `Dockerfile` y la ruta `/health`, con **Auto-Deploy: Off** y despliegue manual.
4. Verificar `/health` = 200 y probar los tres roles.

Para actualizar una instalación existente, ejecutar una vez las migraciones pendientes en orden (`database/migrations/001-reserva-directa.sql`, `002-plazos-atencion.sql`, `003-correos.sql`) antes del despliegue; no ejecutar `schema.sql`. Configurar plazo de pago, antelación y atención en Administración. Las reservas anteriores conservan sus condiciones.

Correos opcionales: verificar el remitente en Brevo y configurar `BREVO_API_KEY`, `EMAIL_FROM`, `APP_BASE_URL` (URL pública de la app), `EMAIL_FROM_NAME` (opcional, TeachMate) y `EMAIL_ENABLED=true` en Render. Por defecto el envío está desactivado; `ADMIN_EMAIL` recibe avisos administrativos. `EMAIL_DAILY_LIMIT` limita las aceptaciones diarias (250 por defecto); el proveedor aplica además su propia cuota. Se usa HTTPS, sin SMTP, conservando las raíces públicas de Java junto a la CA de Aiven. [API de Brevo](https://developers.brevo.com/docs/send-a-transactional-email).

La tabla `correo_pendiente` conserva los avisos con la misma transacción de la reserva o pago. Incluye recordatorios de clase (una hora antes), pago y revisión (30 minutos antes). Se omiten recordatorios obsoletos; los avisos generales caducan tras 24 horas. Reintentos con la misma clave de Brevo dentro de 25 minutos; una respuesta incierta tras una interrupción o fuera de esa ventana requiere revisión manual para evitar duplicados. Administración → Correos y avisos permite revisar, cancelar y reintentar. “Aceptado” no garantiza entrega; no se incluyen comprobantes ni datos bancarios. Las pruebas usan memoria y un servidor simulado local, sin enviar correos reales.

Tomcat procesa vencimientos al iniciar y cada minuto, en lotes de hasta 1000 reservas y 1000 propuestas; recupera los pendientes tras un reinicio. No valida pagos automáticamente. Render Free suspende el servicio después de 15 minutos sin tráfico: mientras esté suspendido no ejecuta tareas. Para procesamiento continuo, usar una instancia que permanezca activa; la comprobación de plazos también se conserva al consultar o gestionar reservas. [Límites de Render Free](https://render.com/docs/free).

Probar el contenedor en local: `docker build -t teachmate .` y `docker run --rm --env-file .env -p 10000:10000 teachmate`.

## Antes de operar

Verificación local: `mvn verify`. Recorridos cubiertos, actualización de la base y ensayo pendiente del despliegue en [Verificación y puesta en marcha](docs/VERIFICACION.md). El diagnóstico `database/verificar-instalacion.sql` es de solo lectura. El rediseño visual corresponde a una fase posterior.

Configurar tarifas, reparto, plazos, antelación mínima, cuenta receptora, instrucciones de cobro, días de corte y políticas de inasistencia. Probar en ensayo todo el flujo (reservas, pagos, reprogramación, devoluciones, liquidaciones), sin pagos ficticios en la base publicada. Respaldar datos y comprobantes (Aiven o `mysqldump` con TLS) y probar la restauración. Volver al código anterior no revierte MySQL.
