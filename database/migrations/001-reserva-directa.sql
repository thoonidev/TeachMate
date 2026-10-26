-- Fase 1: ejecutar UNA VEZ en teachmateweb antes de desplegar el nuevo codigo.
-- No elimina datos ni tablas. Las reservas anteriores conservan sus vencimientos.
-- MySQL confirma implicitamente las instrucciones ALTER TABLE.
USE teachmateweb;

ALTER TABLE tutoria ALTER COLUMN estado SET DEFAULT 'Pendiente de pago';
ALTER TABLE configuracion_reservas DROP CHECK ck_configuracion_plazos;
UPDATE configuracion_reservas
SET respuesta_minutos=0, antelacion_minutos=GREATEST(1440,antelacion_minutos,pago_minutos)
WHERE pago_minutos IS NOT NULL AND antelacion_minutos IS NOT NULL;
ALTER TABLE configuracion_reservas ADD CONSTRAINT ck_configuracion_plazos CHECK (
    (respuesta_minutos IS NULL AND pago_minutos IS NULL AND antelacion_minutos IS NULL) OR
    (respuesta_minutos=0 AND pago_minutos BETWEEN 15 AND 10080
     AND antelacion_minutos BETWEEN 1440 AND 43200 AND antelacion_minutos>=pago_minutos)
);

SELECT respuesta_minutos,pago_minutos,antelacion_minutos FROM configuracion_reservas;
