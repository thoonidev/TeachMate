# Verificación y puesta en marcha

La fase 4 se verifica localmente con Java 17 compatible, H2 en modo MySQL y un servidor HTTP de correo simulado. No utiliza Aiven, cuentas de Brevo ni dinero real. No sustituye una prueba del despliegue en MySQL/Tomcat ni demuestra entrega de correos a una bandeja real.

## Verificación local

Ejecutar `mvn verify`. Las pruebas permanentes están en `src/test/java`; los informes y el WAR se generan en `target/` y no se publican en Git.

| Recorrido | Comprobación |
| --- | --- |
| Reserva directa | Pago habilitado sin aceptación del profesor; antelación, disponibilidad y cruces protegidos. |
| Plazos | Madrugada, cierre del sábado, domingo, margen de revisión y condiciones conservadas por reserva. |
| Pago y corrección | Comprobante en revisión sin confirmar; observación, nueva versión y validación exclusivamente administrativa. |
| Reprogramación | Aceptación de ambos, precio y reparto conservados, enlace anterior retirado. |
| Realización y liquidación | Sesión finalizada y pagada, reparto correcto, una sola liquidación y transferencia registrada por administración. |
| Cancelación y devolución | Solicitud revisada, cancelación separada del dinero, devolución aprobada y transferencia registrada por separado. |
| Devolución posterior | Transferencia anterior intacta; ajuste requiere acuerdo del profesor; no se crea un cierre negativo. |
| Roles y formularios | Terceros sin acceso a reservas/comprobantes; cola solo para administración; solicitudes sin token o con token distinto bloqueadas. |
| Correos | Cola atómica, deduplicación, reintentos, cuota, interrupciones, avisos caducados y proveedor simulado. |
| Recordatorios | Se descartan horarios obsoletos; volver al horario original recupera avisos descartados automáticamente, sin repetir aceptados ni recuperar cancelaciones manuales. |

También se compilan las páginas JSP completas con Tomcat 9 y se comprueba la sintaxis del inicio del contenedor. La fase 5 renueva el estilo compartido, centra las secciones de la portada y reduce las instrucciones a lo necesario. El catálogo vacío no muestra tarjetas ficticias. Los plazos se presentan en horas; administración permite horas decimales o minutos, sin cambiar los valores guardados ni las reglas de reserva.

La comprobación de la fase 5 incluye 68 pruebas aprobadas, compilación de las vistas JSP y vista previa local con H2: portada con y sin catálogo, reserva del alumno, disponibilidad del profesor y configuración de administración. Se revisaron escritorio y anchos móviles de 320 y 390 px; guardar 1.5 horas conserva el plazo de 90 minutos. Los datos de esa vista previa son desechables y no se incorporan al proyecto ni a Aiven.

## Base publicada

1. Para una base **nueva y vacía**, ejecutar solo `database/schema.sql`, que ya incluye las fases 1–3. No aplicar después las migraciones sobre esa misma instalación.
2. Para la base **existente**, ejecutar únicamente las migraciones todavía pendientes, una vez y en orden: `001-reserva-directa.sql`, `002-plazos-atencion.sql`, `003-correos.sql`. Si ya se ejecutó una, continuar con la siguiente. No volver a ejecutar `schema.sql` ni los antiguos scripts de limpieza.
3. Ejecutar `database/verificar-instalacion.sql` en Workbench. Es de solo lectura: se esperan 24 tablas, nueve columnas añadidas en fase 2 y 21 columnas de la cola. Una diferencia indica que hay que revisar la instalación; el script no la modifica.
4. Completar cobro, plazo de pago, antelación y liquidaciones en Administración. La atención acordada es lunes a sábado, 09:00–18:00 Lima, con 120 minutos de revisión. El plazo de pago debe dejar margen dentro de esa atención; publicar una franja compromete al profesor.

El administrador permanece en `ADMIN_EMAIL` y `ADMIN_PASSWORD_HASH` de Render; no se crea ni se reinicia desde estos scripts. Las migraciones no borran profesores, alumnos, reservas ni comprobantes.

## Ensayo del despliegue (pendiente hasta publicar y configurar)

- Publicar manualmente el commit revisado; `/health` debe responder 200. Verificar acceso de los tres roles y bloqueo de páginas administrativas desde cuentas de alumno/profesor.
- En un entorno separado de ensayo, recorrer reserva → comprobante → observación/corrección → validación → enlace → clase → liquidación. Probar también cancelación, devolución y vencimiento. No registrar pagos ficticios en la base destinada a operar.
- Mantener `EMAIL_ENABLED=false` hasta verificar remitente y variables de Brevo. Para validar entrega real, usar destinatarios propios autorizados en el entorno de ensayo; comprobar aceptación en Brevo y recepción. La respuesta “Aceptado” de la cola no demuestra entrega.
- Revisar los avisos pendientes antes de activar correo: los eventos aún vigentes podrían enviarse. Comprobar el límite diario y la pantalla Correos y avisos.
- Render Free puede suspender el proceso: los plazos siguen siendo válidos, pero los trabajos y recordatorios no se ejecutan durante la suspensión. Para continuidad, usar una instancia activa.

No se han ejecutado migraciones ni pruebas contra la base publicada como parte de la verificación local. No se han activado credenciales ni enviado correos reales.
