-- Fase 2: ejecutar UNA VEZ despues de 001-reserva-directa.sql y antes del despliegue.
-- Dias: lunes=1, martes=2, miercoles=4, jueves=8, viernes=16, sabado=32, domingo=64.
-- Configuracion acordada: lunes a sabado, 09:00-18:00 Lima, 120 min de revision.
-- No modifica vencimientos ni condiciones de reservas anteriores.
USE teachmateweb;

ALTER TABLE configuracion_reservas
    ADD COLUMN atencion_dias INT NOT NULL DEFAULT 63,
    ADD COLUMN atencion_desde TIME NOT NULL DEFAULT '09:00:00',
    ADD COLUMN atencion_hasta TIME NOT NULL DEFAULT '18:00:00',
    ADD COLUMN revision_minutos INT NOT NULL DEFAULT 120,
    ADD CONSTRAINT ck_atencion_reservas CHECK(atencion_dias BETWEEN 1 AND 127
        AND atencion_desde>='00:00:00' AND atencion_hasta<='23:59:00'
        AND atencion_desde<atencion_hasta
        AND revision_minutos BETWEEN 15 AND 10080);

ALTER TABLE tutoria
    ADD COLUMN revision_dias INT NULL,
    ADD COLUMN revision_desde TIME NULL,
    ADD COLUMN revision_hasta TIME NULL,
    ADD COLUMN revision_minutos INT NULL,
    ADD COLUMN revision_prevista DATETIME NULL;

CREATE INDEX idx_cambio_vencimiento ON tutoria_cambio(estado,vence_en);
SELECT atencion_dias,atencion_desde,atencion_hasta,revision_minutos FROM configuracion_reservas;
