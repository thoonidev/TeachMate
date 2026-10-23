# TeachMate

Sistema para un equipo de profesores que ofrece asesorías individuales remotas, coordinado por un administrador. Un profesor puede enseñar varios cursos y cada curso puede tener varios profesores.

## Proceso del servicio

1. Administración registra profesores y cursos, asigna cursos y configura tarifas, porcentajes, plazos e instrucciones de cobro. Los profesores publican su disponibilidad.
2. El alumno crea su cuenta, completa su teléfono, elige curso, profesor, duración y horario, e indica su nivel y tema.
3. La solicitud retiene temporalmente ambas agendas. El profesor acepta o rechaza dentro del plazo.
4. Aceptar habilita el pago; todavía no confirma la clase. El alumno transfiere a la cuenta del equipo y adjunta su comprobante privado.
5. Administración verifica el ingreso real y confirma la asesoría. Puede observar el comprobante y conceder el plazo de corrección configurado, sin superar el inicio.
6. El profesor publica el enlace de la videollamada y, al terminar, registra realización, seguimiento o incidencia.
7. Administración prepara el cierre semanal del profesor, revisa el detalle y registra la transferencia realizada fuera del sistema.

Importes en soles (PEN). Fechas, horarios y vencimientos en `America/Lima`. No hay precios, porcentajes, cuentas receptoras ni días de liquidación de ejemplo habilitados como condiciones reales.

## Funcionalidades por rol

| Administrador | Profesor | Alumno |
| --- | --- | --- |
| Profesores activos e inactivos, cursos y tarifas versionadas | Perfil, presentación, niveles y disponibilidad | Registro y perfil reutilizable |
| Asignaciones de varios cursos y supervisión de horarios | Respuesta a solicitudes propias | Oferta de cursos y horarios reservables |
| Condiciones de reserva y cobro | Enlace privado, realización e incidencias | Reservas, pagos y seguimiento propios |
| Validación y observación de comprobantes | Reprogramación por acuerdo de ambos participantes | Comprobantes privados y solicitudes de devolución |
| Cancelaciones y devoluciones totales o parciales | Ingresos, cierres y transferencias propios | Acceso al enlace de la clase confirmada |
| Liquidaciones, ajustes y movimientos del equipo | Aceptación o revisión de ajustes propuestos | Consulta del historial de sus operaciones |

## Reglas principales

- Solicitud, pago del alumno, realización de la sesión y transferencia al profesor tienen estados separados. Una acción no acredita automáticamente las demás.
- Los horarios contiguos se permiten. Se impiden cruces de alumno y profesor, incluso con solicitudes simultáneas. Las retenciones vencidas dejan de bloquear nuevas reservas.
- Un comprobante enviado a tiempo conserva el horario durante su revisión. Si llega el inicio sin validación, el caso pasa a incidencia; un ingreso tardío no desplaza otra reserva.
- Reprogramar conserva el horario original hasta que ambos aceptan y se verifica nuevamente la disponibilidad. Se mantienen precio, reparto y pago.
- Las solicitudes de cancelación o devolución posteriores al pago se revisan por administración. No hay penalización automática de 24 horas ni devoluciones automáticas.
- Las liquidaciones incluyen sesiones realizadas, pagadas y sin asuntos pendientes, una sola vez. La parte del profesor se redondea con `HALF_UP` a dos decimales; la retención del equipo no equivale a utilidad neta ni sueldo administrativo.
- Una devolución posterior no reescribe una transferencia histórica. Administración puede asumir el costo o proponer un ajuste; una reducción exige aceptación del profesor. No se registra una transferencia negativa.
- Los registros anteriores conservan sus datos. No se inventan horarios, tarifas, pagos ni remuneraciones. `Aceptada` se identifica como un estado anterior al flujo actual.

## Interfaz y seguridad

Portada centrada en cursos y profesores, navegación por tareas, menú de cuenta con rol, formularios etiquetados y pantallas adaptadas a celular. Se retiraron el video y las imágenes promocionales anteriores.

Contraseñas PBKDF2, consultas parametrizadas, sesiones renovadas al ingresar, protección CSRF, escape HTML y acceso por rol y propiedad. Comprobantes de hasta 5 MB en PDF, JPG o PNG, almacenados en MySQL y descargados únicamente por su alumno o administración; no dependen del disco temporal de Render. Los enlaces de clase son privados.

## Ejecutar localmente

Se requieren Java 17, Maven, Tomcat 9 y MySQL 8.

1. Crear y seleccionar una base **nueva y vacía**, e importar [database/schema.sql](database/schema.sql). El [catálogo inicial](database/catalog.sql) es opcional y no crea tarifas ni cuentas de prueba.
2. Configurar las variables de [.env.example](.env.example) en el proceso que inicia Tomcat. La aplicación no carga ese archivo automáticamente. Usar `SESSION_COOKIE_SECURE=false` para HTTP local.
3. Generar el hash del administrador con la herramienta indicada en la [guía de despliegue](deploy/README.md).
4. Compilar con `mvn clean package` y copiar `target/TeachMate.war` a `webapps` de Tomcat.
5. Abrir `http://localhost:8080/TeachMate/` y comprobar `/TeachMate/health`.

Las verificaciones de las fases se realizan en entornos temporales aislados. Por solicitud del responsable no se conservan sus archivos ni dependencias exclusivas; `mvn clean package` compila y empaqueta, pero no ejecuta una suite de pruebas eliminada.

## Publicación y actualización

El servicio existente es [TeachMate en Render](https://teachmate-7wu8.onrender.com). Tener una URL publicada no significa que esta entrega ya esté desplegada.

La imagen usa Java 17 y Tomcat 9, con MySQL externo en Aiven. `/health` comprueba conexión y estructura mediante una consulta sin leer datos de clientes; responde 200 o 503. Las migraciones se aplican expresamente antes del nuevo código, nunca al arrancar.

Para una base existente, revisar primero [verificar-despliegue.sql](database/verificar-despliegue.sql) y seguir la [guía de actualización y recuperación](deploy/README.md). **No importar `schema.sql` sobre una base con datos ni repetir migraciones ya aplicadas.**

El Blueprint deja los despliegues automáticos desactivados para coordinar los cambios de estructura. La configuración del servicio existente también debe revisarse en el panel de Render antes de integrar la entrega.

## Alcance y operación

Esta versión atiende a un solo equipo y asesorías de un alumno con un profesor. Cobros, devoluciones y transferencias se ejecutan externamente y después se registran. No incluye clases grupales, pasarela bancaria, nómina, cálculo de impuestos ni utilidad neta.

El seguimiento se consulta en los paneles e historiales; no se han configurado correos, WhatsApp ni recordatorios automáticos. Administración debe revisar solicitudes, comprobantes e incidencias pendientes. El cierre semanal también se prepara manualmente.

Antes de operar con alumnos, completar las condiciones reales y comprobar los accesos, horarios, cuenta receptora y forma de contacto del equipo.

## Resumen de las fases

| Fase | Resultado |
| --- | --- |
| 1 | Reglas del servicio individual remoto, responsabilidades y limpieza inicial. |
| 2 | Diseño compartido, portada, navegación por tareas y componentes adaptados a celular. |
| 3 | Cursos relacionados con profesores, tarifas y reparto versionados, perfiles y disponibilidad. |
| 4 | Reservas con vencimientos, detalle privado, reprogramación, asistencia, incidencias e historial. |
| 5 | Comprobantes privados, validación de ingresos, correcciones y devoluciones registradas por separado. |
| 6 | Cierres semanales, transferencias, consulta de ingresos y ajustes aceptados por el profesor. |
| 7 | Revisión integral, comprobación de migraciones y recuperación, documentación final y preparación de la publicación. |
## Validación de esta entrega

El 23/10/2026 se ejecutaron 138 comprobaciones en MySQL 8 y Tomcat 9 locales aislados: recorridos de los tres roles, migraciones desde dos estructuras anteriores, concurrencia, acceso privado y recuperación de datos financieros y de un comprobante binario de 3 MB. Se validaron también la compilación y ocho casos del arranque con un proceso de servidor simulado.

La imagen Docker completa no se construyó en este equipo porque Docker no está disponible. La aplicación publicada y la base de Aiven requieren su propia comprobación y respaldo antes de actualizarse. Esta validación local no acredita una publicación en producción.