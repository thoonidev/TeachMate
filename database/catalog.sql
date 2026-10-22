-- Optional initial catalog. No demo accounts or passwords are created.
INSERT INTO especialidad (especialidad_nombre)
SELECT catalog.nombre
FROM (
    SELECT 'Aritmetica' AS nombre UNION ALL SELECT 'Fisica'
    UNION ALL SELECT 'Algebra' UNION ALL SELECT 'Quimica' UNION ALL SELECT 'Geometria'
) AS catalog
WHERE NOT EXISTS (
    SELECT 1 FROM especialidad e WHERE e.especialidad_nombre = catalog.nombre
);
