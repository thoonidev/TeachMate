-- READ ONLY. Run against the explicitly selected database before and after an update.
-- A marker identifies a migration to inspect; it does NOT certify that a partially failed script completed.
SELECT DATABASE() AS base_seleccionada, VERSION() AS version_mysql;
SELECT 'alumnos' AS entidad,COUNT(*) AS filas FROM user_details
UNION ALL SELECT 'profesores',COUNT(*) FROM docente
UNION ALL SELECT 'asesorias',COUNT(*) FROM tutoria
UNION ALL SELECT 'cursos',COUNT(*) FROM especialidad;

SELECT 'security-migration.sql' AS migracion,
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='user_details' AND column_name='clave' AND character_maximum_length>=255) AS marcador_presente
UNION ALL SELECT 'tutorias-migration.sql',
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='tutoria' AND column_name='hora_inicio')
UNION ALL SELECT 'oferta-migration.sql',
    EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='docente_curso')
UNION ALL SELECT 'gestion-migration.sql',
    EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='tutoria_evento')
UNION ALL SELECT 'pagos-migration.sql',
    EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='pago_alumno')
UNION ALL SELECT 'liquidaciones-migration.sql',
    EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='liquidacion');

-- Full inventory for detecting partial migrations; compare with each script, not only its marker.
SELECT table_name,column_name,column_type,is_nullable,column_default
FROM information_schema.columns WHERE table_schema=DATABASE() ORDER BY table_name,ordinal_position;
SELECT table_name,constraint_name,constraint_type
FROM information_schema.table_constraints WHERE constraint_schema=DATABASE()
ORDER BY table_name,constraint_type,constraint_name;
SELECT estado,COUNT(*) AS filas FROM tutoria GROUP BY estado;
SELECT email,COUNT(*) AS duplicados FROM user_details GROUP BY email HAVING COUNT(*)>1;
SELECT email,COUNT(*) AS duplicados FROM docente GROUP BY email HAVING COUNT(*)>1;
SELECT LOWER(TRIM(especialidad_nombre)) AS curso,COUNT(*) AS duplicados
FROM especialidad GROUP BY LOWER(TRIM(especialidad_nombre)) HAVING COUNT(*)>1;
