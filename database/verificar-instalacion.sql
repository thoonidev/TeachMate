-- Diagnostico SOLO DE LECTURA para Workbench. No elimina ni crea datos.
-- Base existente: comprobar despues de las migraciones pendientes 001, 002 y 003.
USE teachmateweb;
SELECT DATABASE() AS base_actual, VERSION() AS version_mysql;

SELECT 'Tablas TeachMate' AS control, COUNT(*) AS encontrado, 24 AS esperado
FROM information_schema.tables
WHERE table_schema=DATABASE() AND table_type='BASE TABLE' AND table_name IN (
    'user_details','especialidad','docente','docente_curso','tarifa_curso','disponibilidad',
    'tutoria','configuracion_reservas','tutoria_evento','tutoria_cambio','configuracion_pagos',
    'pago_alumno','pago_comprobante','pago_identificador','pago_evento','pago_devolucion',
    'configuracion_liquidaciones','liquidacion','liquidacion_ajuste','liquidacion_linea',
    'liquidacion_sesion','liquidacion_ajuste_asignacion','liquidacion_evento','correo_pendiente'
);

SELECT 'Columnas nuevas fase 2' AS control, COUNT(*) AS encontrado, 9 AS esperado
FROM information_schema.columns WHERE table_schema=DATABASE() AND (
    (table_name='tutoria' AND column_name IN ('revision_dias','revision_desde','revision_hasta','revision_minutos','revision_prevista')) OR
    (table_name='configuracion_reservas' AND column_name IN ('atencion_dias','atencion_desde','atencion_hasta','revision_minutos'))
);

SELECT 'Columnas cola de correos' AS control, COUNT(*) AS encontrado, 21 AS esperado
FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='correo_pendiente';

-- Revisar ck_configuracion_plazos: respuesta_minutos=0 y antelacion minima de 1440.
SELECT cc.constraint_name,cc.check_clause
FROM information_schema.check_constraints cc
JOIN information_schema.table_constraints tc
ON tc.constraint_schema=cc.constraint_schema AND tc.constraint_name=cc.constraint_name
WHERE tc.table_schema=DATABASE() AND tc.table_name='configuracion_reservas';

SELECT id,pago_minutos,antelacion_minutos,atencion_dias,atencion_desde,atencion_hasta,revision_minutos
FROM configuracion_reservas;

-- Comprueba configuracion de cobro sin mostrar cuentas ni instrucciones bancarias.
SELECT id,CASE WHEN medio IS NOT NULL AND TRIM(medio)<>''
    AND titular IS NOT NULL AND TRIM(titular)<>'' AND destino IS NOT NULL AND TRIM(destino)<>''
    AND instrucciones IS NOT NULL AND TRIM(instrucciones)<>'' AND correccion_minutos IS NOT NULL
    THEN 'Completa' ELSE 'Pendiente' END AS configuracion_cobro
FROM configuracion_pagos;
SELECT id,corte_dia,pago_dia FROM configuracion_liquidaciones;

SELECT 'alumnos' AS tabla,COUNT(*) AS registros FROM user_details
UNION ALL SELECT 'profesores',COUNT(*) FROM docente
UNION ALL SELECT 'cursos',COUNT(*) FROM especialidad
UNION ALL SELECT 'reservas',COUNT(*) FROM tutoria
UNION ALL SELECT 'pagos',COUNT(*) FROM pago_alumno
UNION ALL SELECT 'liquidaciones',COUNT(*) FROM liquidacion
UNION ALL SELECT 'avisos',COUNT(*) FROM correo_pendiente;
