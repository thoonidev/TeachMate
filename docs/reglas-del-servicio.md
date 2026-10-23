# TeachMate: reglas del servicio y alcance de implementación

Documento de decisiones de las fases 1 a 7, consolidado con la entrega local. Las condiciones concretas deben configurarse por el equipo. El README y la guía de despliegue describen el comportamiento implementado y los requisitos para publicar; este documento no acredita que la entrega esté desplegada.

## 1. Alcance

- Un equipo de profesores, coordinado por un administrador.
- Asesorías individuales remotas: una sesión corresponde a un alumno, un profesor y un curso.
- Un profesor puede enseñar varios cursos; un curso puede ser impartido por varios profesores.
- Videollamadas en servicios externos mediante un enlace de acceso.
- Cobro externo al alumno, validación manual del administrador y transferencias externas a profesores.
- Liquidación semanal propuesta; la fecha de corte y el día de pago deben configurarse antes de operar.
- Moneda inicial: soles (PEN). Fechas y horarios: America/Lima.
- Fuera de esta versión: grupos, múltiples equipos independientes, videollamadas propias, pasarela de cobro, transferencias automáticas, nómina y contabilidad fiscal completa.

## 2. Responsabilidades

| Operación | Administrador | Profesor | Alumno |
| --- | --- | --- | --- |
| Registrar y desactivar profesores | Sí; conserva historial | No | No |
| Gestionar cursos, tarifas y asignaciones | Sí | Consulta sus asignaciones | Consulta oferta activa |
| Definir disponibilidad | Supervisa y ayuda; cambios trazables | Gestiona la propia | Consulta horarios reservables |
| Crear cuenta de alumno | Puede asistir al alumno | No | Registro propio |
| Solicitar una asesoría | Puede asistir; registra al actor | No en nombre de otro profesor | Para sí mismo |
| Aceptar o rechazar | Supervisa; no suplanta al profesor | Solo solicitudes propias | No |
| Registrar comprobante | Puede asistir; registra al actor | No | Solo reservas propias |
| Validar ingreso y registrar devolución | Sí | Consulta estado de sus sesiones | Consulta sus pagos |
| Registrar enlace y resultado de clase | Resuelve incidencias con motivo | Solo sesiones propias | Consulta sus sesiones |
| Liquidar al profesor | Sí | Consulta sus propios importes | No |

El rol se identifica en el menú de cuenta, no como opción de navegación. Ser propietario del equipo no implica permisos de administrador en una cuenta de profesor. Se conservan inicialmente cuentas separadas por rol; el acceso combinado no forma parte de esta versión.

## 3. Preparación del servicio

1. El administrador registra cursos y profesores y establece sus relaciones.
2. Configura tarifas por curso y duración, reparto y condiciones comerciales.
3. El profesor completa su presentación, niveles que atiende y disponibilidad.
4. Un curso solo permite reservas si existe profesor activo asignado, horario disponible, tarifa válida y configuración comercial completa.
5. No se precargan precios ni porcentajes de ejemplo como valores reales. Tarifas y reparto conservan versiones y fecha de vigencia.

## 4. Reserva individual

1. El alumno elige curso, profesor y un horario dentro de su disponibilidad.
2. Indica tema, nivel y duración habilitada. Nombre y contacto provienen del perfil; no se repiten género, edad ni dirección en cada reserva.
3. Revisa fecha, zona horaria, precio total, condiciones y vencimientos antes de solicitar.
4. Al enviar se crea una solicitud pendiente de respuesta y una retención temporal del horario para alumno y profesor.
5. Solo el profesor asignado acepta o rechaza. Rechazar requiere un motivo breve.
6. Aceptar habilita el pago; todavía no confirma la clase.
7. El alumno paga y registra comprobante dentro del plazo. El administrador verifica el ingreso real.
8. El pago validado confirma la asesoría. El profesor añade el enlace de Meet o Zoom.
9. Al finalizar, el profesor registra realización o incidencia y seguimiento.

Revisar disponibilidad y reservar deben ser una operación atómica: también se impiden envíos simultáneos y duplicados. Los horarios contiguos se permiten. Una retención vencida no bloquea nuevas reservas aunque todavía no se haya ejecutado una tarea de limpieza.

No se eliminan reservas con historial para liberar un horario. La desactivación de profesor impide nuevas reservas y muestra al administrador las sesiones futuras por resolver; no borra ni cancela automáticamente las ya confirmadas.

## 5. Estados independientes

Los estados siguientes se implementaron mediante las migraciones de gestión y pagos. El estado Aceptada se conserva para registros anteriores, sin presumir un ingreso validado.

| Asesoría | Significado |
| --- | --- |
| Pendiente de respuesta | Espera decisión del profesor; retención con vencimiento |
| Pendiente de pago | Profesor aceptó; espera pago o revisión del comprobante |
| Confirmada | Ingreso validado y horario confirmado |
| Completada | Sesión realizada, luego de su hora de finalización |
| Rechazada | Profesor no acepta; horario liberado |
| Vencida | Se agotó el plazo de respuesta o pago |
| Cancelada | Se cancela conservando motivo, responsable y efectos económicos |
| Incidencia | Inasistencia u otro problema pendiente de resolución administrativa |

| Pago del alumno | Significado |
| --- | --- |
| Pendiente | No hay pago validado |
| En revisión | Comprobante recibido a tiempo; espera verificación |
| Observado | El comprobante no permite validar; requiere explicación y plazo de corrección |
| Validado | Ingreso confirmado por el administrador |
| Devolución pendiente | Se aprobó devolver dinero; todavía no se registra la devolución |
| Devuelto | Devolución ejecutada y documentada |

La ausencia de pago en solicitudes rechazadas o vencidas no equivale a una deuda. No se muestra una obligación de pago en esos casos.

Una cancelación no marca automáticamente el dinero como devuelto. Un pago validado tampoco marca automáticamente la clase como realizada ni al profesor como pagado.

## 6. Plazos y comunicaciones

- Los tiempos de respuesta, pago, corrección del comprobante y antelación mínima de reserva serán configuración obligatoria, no números inventados.
- La antelación mínima debe dar tiempo a la respuesta y al pago antes del inicio de la clase.
- Un comprobante recibido a tiempo mantiene la retención mientras se revisa. El panel debe alertar sobre revisión atrasada y proximidad del inicio; al llegar al inicio sin validar pasa a incidencia y requiere resolución, sin mantener una solicitud bloqueada indefinidamente.
- Una reserva vencida no se reactiva si su horario ya se ocupó. Un ingreso tardío se resuelve mediante devolución o nueva reserva aceptada por el alumno.
- Solicitudes, respuestas, confirmaciones y cambios se consultan en los paneles e historiales. Correos, WhatsApp y recordatorios automáticos quedan como ampliación: no hay un proveedor configurado en esta entrega. El equipo debe revisar sus pendientes.
- El enlace de la clase solo es visible para participantes y administrador. Los comprobantes no se publican ni se exponen a otros alumnos o profesores.

## 7. Cambios, cancelaciones e incidencias

- La reprogramación conserva el horario original hasta confirmar el nuevo. Al confirmar se valida disponibilidad y se cambia de forma atómica, conservando el historial.
- Cambiar profesor requiere asignación válida al curso y aceptación de los involucrados. No se incrementa el precio sin aceptación expresa del alumno.
- Una nueva tarifa o reparto no modifica una reserva ya confirmada. El pago de la reserva se reutiliza al reprogramar sin generar cobro ni remuneración duplicados.
- Cancelar una solicitud sin pago libera el horario y no genera ingreso al profesor.
- Si el profesor cancela una sesión pagada y no hay alternativa aceptada, corresponde tramitar la devolución.
- Inasistencias del alumno o profesor y reclamos bloquean la liquidación de la sesión hasta su resolución. No se aplica penalización ni se reconoce pago automático por inasistencia sin una política acordada.
- Decisión confirmada: las cancelaciones y devoluciones solicitadas por el alumno después de pagar pasan por revisión del administrador. El alumno registra el motivo y consulta la respuesta; enviar la solicitud no cancela ni devuelve dinero automáticamente.
- No se aplica una regla automática de 24 horas ni penalizaciones automáticas. El administrador registra la decisión y su motivo; si aprueba una devolución, registra el importe y después la fecha y referencia de la transferencia efectivamente realizada.
- Mientras se resuelve la solicitud, la remuneración de esa sesión queda retenida para evitar liquidarla durante una disputa. El resultado se comunica al alumno y al profesor. Estas condiciones deben mostrarse antes de reservar.
- Toda excepción administrativa conserva actor, fecha y motivo; no se borra el historial.

## 8. Reparto y liquidación

- El profesor genera remuneración por sesiones realizadas, pagadas y sin incidencia pendiente. Si no realiza sesiones no genera pago por asesorías.
- El porcentaje se acuerda antes de operar. El 80/20 mencionado en la conversación es ilustrativo y no queda aprobado ni precargado.
- Se guarda una copia de precio y reparto por reserva desde la solicitud; se muestra antes de que el profesor acepte y se mantiene durante su confirmación y ejecución.
- Los importes usan decimales exactos, dos decimales y una regla única de redondeo: HALF_UP para la parte del profesor; la parte del equipo es el total menos esa parte. No se utilizan flotantes para dinero.
- El porcentaje retenido por el equipo cubre operación; no es automáticamente remuneración del administrador ni ganancia neta.
- El cierre semanal reúne sesiones elegibles hasta la fecha de corte y excluye sesiones ya liquidadas. Cada línea de remuneración pertenece como máximo a una liquidación activa.
- El administrador revisa el detalle, realiza la transferencia fuera del sistema y registra importe, fecha y referencia. Solo entonces queda pagada.
- Cada profesor consulta generado, pendiente y pagado, con detalle de sesiones y ajustes.
- Una devolución posterior se registra con un ajuste vinculado a la reserva y a su liquidación original; nunca reescribe un pago histórico ni descuenta silenciosamente dinero.
- Esta versión no calcula sueldos administrativos, impuestos ni utilidad neta. Los reportes distinguen recaudación, devoluciones, importe del profesor y retención del equipo.

## 9. Cambios respecto de la versión inicial

| Situación inicial | Resultado implementado | Fase |
| --- | --- | --- |
| Especialidad de texto única por profesor | Cursos relacionados con varios profesores | 3 |
| Fecha y hora propuestas libremente | Disponibilidad reservable y retención con vencimiento | 3 y 4 |
| Datos personales en cada solicitud | Perfil reutilizable y reserva reducida | 3 y 4 |
| Aceptada equivale a sesión confirmada | Separar aceptación de confirmación por pago | 4 y 5 |
| Sin tarifas ni comprobantes | Precio histórico, registro y validación de pago | 5 |
| Sin lugar de clase remoto | Enlace privado para cada sesión confirmada | 4 |
| Sin cambios de horario ni asistencia | Reprogramación trazable y resolución de incidencias | 4 |
| Sin reparto ni liquidaciones | Remuneraciones por sesión y cierre semanal | 6 |
| Menú con rol y enlaces mezclados, footer mínimo | Navegación por tareas y componentes consistentes | 2 a 6 |
| Video promocional en portada | Retirarlo y priorizar cursos y profesores reales | 1 y 2 |

Se preservan controles de acceso, protección de formularios, contraseñas protegidas, validación de cruces e historial. Los datos antiguos sin precio, pago u horario completo no reciben valores inventados ni entran automáticamente en liquidaciones nuevas.

## 10. Criterios de validación

1. Dos alumnos no pueden ocupar el mismo horario de un profesor mediante envíos simultáneos.
2. Un alumno no puede reservar sesiones superpuestas con distintos profesores.
3. Un profesor no asignado al curso o inactivo no puede recibir nuevas solicitudes.
4. Aceptar una solicitud no confirma el pago; subir comprobante tampoco lo valida.
5. Un horario vencido vuelve a estar disponible y un pago tardío no desplaza otra reserva.
6. Reprogramar no pierde el horario original si falla la nueva reserva.
7. Una sesión no realizada o con incidencia no se liquida automáticamente.
8. Una sesión completada y pagada entra una sola vez en liquidación.
9. Cambiar una tarifa o porcentaje no modifica reservas anteriores.
10. Profesor y alumno solo acceden a sus propios datos; comprobantes y enlaces son privados.
11. En celular se puede encontrar curso, reservar, consultar pago y entrar a clase sin depender de una tabla horizontal.
12. Antes de publicar se verifican migración con datos históricos, recuperación y recorrido completo de los tres roles.

## 11. Decisiones pendientes del responsable del servicio

Estas decisiones no se presentan como aprobadas y no deben bloquear el trabajo de diseño independiente:

- Valores de tarifas, duraciones ofrecidas y porcentaje de reparto: el responsable confirmó que deben configurarse antes de habilitar reservas. Los valores concretos se ingresarán al preparar el servicio.
- Plazos de respuesta, pago, corrección y antelación mínima.
- Compensación ante inasistencia del alumno, y tratamiento de comisiones en devoluciones.
- Día de corte y día de transferencia semanal; propuesta ilustrativa: domingo y martes.
- Cuenta receptora, instrucciones de cobro, contacto de soporte y datos comerciales reales.
- Niveles y público atendido; si se admiten menores, definir responsable de la cuenta y comunicación con su representante antes de habilitar ese flujo.

Las fases de implementación incluyen reservas, pagos y liquidaciones. Los valores comerciales reales siguen siendo responsabilidad del equipo y no se inventan durante la validación. Publicar requiere revisar la estructura real, un respaldo comprobado y el commit que utiliza Render.

## 12. Limpieza y entrega

- No actualizar README.md ni deploy/README.md hasta finalizar todas las fases.
- No conservar capturas, scripts o archivos temporales usados para validar fases.
- Retirar imágenes, videos y código únicamente junto con sus referencias o cuando se sustituya la funcionalidad correspondiente.
- Conservar migraciones y configuración de despliegue que siguen siendo necesarias para instalaciones existentes.
- Eliminar los archivos de pruebas de fases solicitados por el responsable y sus dependencias exclusivas; las comprobaciones futuras no dejarán artefactos temporales en el repositorio.
