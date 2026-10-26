# TeachMate

Sistema web para un equipo de profesores que da asesorías individuales remotas, coordinado por un administrador. Gestiona cursos, disponibilidad, reservas, pagos y liquidaciones. Stack: Java 17, Maven, JSP/Servlets, Tomcat 9, MySQL 8.0.16+. Importes en PEN; horarios en `America/Lima`.

## Roles

| Rol           | Funciones                                                                                                 |
| ------------- | --------------------------------------------------------------------------------------------------------- |
| Administrador | Profesores, cursos, tarifas, condiciones, revisión de pagos, cancelaciones, devoluciones y liquidaciones. |
| Profesor      | Perfil, disponibilidad, solicitudes, enlace de videollamada, resultado de sesión e ingresos.              |
| Alumno        | Registro, reservas, comprobantes y solicitudes de cancelación o devolución.                               |

Videollamadas, cobros y transferencias son externos; TeachMate solo registra el seguimiento. No incluye clases grupales, pasarela de pago, transferencias automáticas, nómina, contabilidad ni avisos automáticos (correo/WhatsApp).

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

Para actualizar una instalación anterior a la reserva directa, ejecutar una vez `database/migrations/001-reserva-directa.sql` antes del despliegue (no ejecutar `schema.sql` sobre la base existente). Configurar plazo de pago y antelación en Administración. Esta fase mantiene la validación manual del ingreso, los plazos corridos y la revisión desde los paneles; todavía no incorpora correos ni vencimientos periódicos.

Probar el contenedor en local: `docker build -t teachmate .` y `docker run --rm --env-file .env -p 10000:10000 teachmate`.

## Antes de operar

Configurar tarifas, reparto, plazos, antelación mínima, cuenta receptora, instrucciones de cobro, días de corte y políticas de inasistencia. Probar en ensayo todo el flujo (reservas, pagos, reprogramación, devoluciones, liquidaciones), sin pagos ficticios en la base publicada. Respaldar datos y comprobantes (Aiven o `mysqldump` con TLS) y probar la restauración. Volver al código anterior no revierte MySQL.
