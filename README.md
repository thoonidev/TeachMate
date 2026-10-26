# TeachMate

Asesorías individuales online con reservas, pagos, seguimiento y liquidaciones. Java 17, Maven, JSP/Servlets, Tomcat 9 y MySQL 8.0.16+. Precios en soles; horarios de Lima, Perú.

## Uso

| Rol | Funciones |
| --- | --- |
| Administrador | Cursos, profesores, tarifas, plazos, revisión de pagos y liquidaciones. |
| Profesor | Disponibilidad, clases, enlaces de videollamada e ingresos. |
| Alumno | Reservas, comprobantes y solicitudes de cambio o cancelación. |

1. Administración configura cursos, tarifas, cobros y plazos; el profesor publica sus horarios.
2. El alumno reserva directamente un horario disponible y paga dentro del plazo indicado.
3. Administración verifica el ingreso y confirma la clase; el profesor publica el enlace y registra el resultado.
4. Administración registra las devoluciones y las transferencias al profesor cuando correspondan.

## Reglas principales

- Anticipación mínima de **24 horas**, configurable. El sistema también deja tiempo para pagar y revisar el comprobante.
- Atención inicial: **lunes a sábado, 09:00–18:00 Lima**, con **2 horas de revisión**. El plazo de pago cuenta también de noche y en fines de semana; la revisión solo cuenta dentro del horario de atención. No se contemplan feriados automáticamente.
- Publicar un horario compromete al profesor; no debe aceptar cada reserva. No se permiten cruces y las reservas vencidas liberan el horario.
- Subir un comprobante no confirma la clase. Si sigue sin validar al inicio, pasa a incidencia. Cada reserva conserva sus plazos, precio y reparto.
- Reprogramar requiere acuerdo de ambos. Administración revisa cancelaciones y devoluciones; cancelar no devuelve dinero automáticamente.
- Solo se liquidan clases realizadas, pagadas y sin pendientes, una vez. Los ajustes a la baja requieren acuerdo del profesor y no modifican transferencias anteriores.
- Cobros, transferencias y videollamadas son externos. Los comprobantes privados PDF/JPG/PNG (hasta 5 MB) se guardan en MySQL.

## Instalación

1. Crear y seleccionar una base **sin tablas**, con `utf8mb4`, y ejecutar **`database/schema.sql` una sola vez**. Es el único SQL: incluye las 24 tablas y las tres filas de configuración; no crea datos de prueba ni administrador. No actualiza tablas existentes. Borrar registros no equivale a borrar las tablas.
2. Configurar las variables de `.env.example` en el proceso que inicia Tomcat. La aplicación no carga ese archivo automáticamente.

   | Variables | Uso |
   | --- | --- |
   | `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexión MySQL; usuario de aplicación con SELECT, INSERT, UPDATE y DELETE. |
   | `ADMIN_EMAIL`, `ADMIN_PASSWORD_HASH` | Acceso administrativo; conservar los actuales en Render. |
   | `DB_CA_CERT_BASE64` | Certificado CA de Aiven en Base64; vacío para MySQL local. |
   | `SESSION_COOKIE_SECURE`, `TRUST_RENDER_PROXY` | `false` en local y `true` en Render. |
   | `PORT` | Puerto del contenedor, por defecto `10000`. |

3. Ejecutar `mvn clean verify`. Para Tomcat local, copiar `target/TeachMate.war` a `webapps/`: `http://localhost:8080/TeachMate/`.
4. Solo para un administrador nuevo, generar el hash con `java -cp "target/classes;target/TeachMate/WEB-INF/lib/*" com.tme.security.PasswordTool hash`. No guardar contraseñas ni claves en Git.

## Actualizar Render y Aiven

1. Subir la rama revisada a GitHub e integrarla en la rama que despliega el servicio. Las cinco fases están encadenadas; la rama final contiene los cambios anteriores.
2. **Antes de desplegar**, comprobar la estructura de Aiven. Si aún tiene las tablas antiguas, necesita actualizarse: este proyecto conserva un único esquema para instalación desde cero. No ejecutar `schema.sql` sobre las tablas existentes. La limpieza del repositorio no modifica Aiven.
3. Conservar las credenciales de Render. En `DB_URL`, usar TLS con `sslMode=VERIFY_IDENTITY` y configurar la CA de Aiven.
4. Usar el `Dockerfile`, mantener despliegue manual y seleccionar el commit actualizado. Comprobar `/health` = 200 y el acceso de los tres roles. No es necesario crear otro servicio.
5. Completar en Administración los plazos de pago y corrección, atención, cobro, días de corte y pago; registrar cursos, tarifas, profesores y disponibilidad.

El administrador depende de las variables de Render, no de una tabla. Reconstruir la base reinicia las configuraciones; habrá que completarlas otra vez.

## Correos opcionales

Verificar el remitente en [Brevo](https://developers.brevo.com/docs/send-a-transactional-email) y configurar en Render:

- `BREVO_API_KEY`, `EMAIL_FROM` y `APP_BASE_URL` (URL pública de TeachMate).
- `EMAIL_FROM_NAME` (opcional; TeachMate) y `EMAIL_DAILY_LIMIT` (250 por defecto; sujeto también a la cuota del proveedor).
- Mantener `EMAIL_ENABLED=false` hasta completar la configuración y probar la recepción; activarlo con `true`. `ADMIN_EMAIL` recibe los avisos administrativos.

Administración → Correos y avisos permite revisar, cancelar y reintentar los envíos. “Aceptado” indica recepción por el proveedor, no entrega al destinatario. Revisar en Brevo los envíos dudosos antes de reintentar y revisar la cola pendiente antes de activarla. No se envían comprobantes ni datos bancarios. Hay recordatorios de clase, pago y revisión.

## Verificación y operación

- `mvn verify`: 68 pruebas de reservas, plazos, pagos, reprogramaciones, devoluciones, liquidaciones, permisos y correos simulados. Los informes y el WAR quedan en `target/`, fuera de Git.
- Vistas JSP compiladas y diseño revisado localmente en escritorio y móvil, con catálogo y sin registros. Estilo en azul tinta, marfil y coral, mensajes breves y plazos en horas.
- Pendiente tras publicar: probar el flujo completo en un entorno de ensayo y comprobar la recepción real de correos. Las pruebas locales usan H2 y un proveedor simulado; no verifican Aiven ni la bandeja real.
- Vencimientos y correos se procesan periódicamente mientras el servicio está activo. **Render Free suspende la aplicación tras 15 minutos sin tráfico**; durante la suspensión no ejecuta esos trabajos. Para continuidad, usar una instancia que permanezca activa. [Limitaciones de Render Free](https://render.com/docs/free).
